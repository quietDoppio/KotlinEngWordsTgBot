
import dictionary.FileUserDictionary
import dictionary.LearnWordsTrainer
import dictionary.Question
import dictionary.STATISTIC_TO_SEND
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val fileUserDictionary = FileUserDictionary()
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    val json = Json { ignoreUnknownKeys = true }
    var responseString: String
    var lastUpdateId: Long = 0

    while (true) {
        Thread.sleep(2000)
        responseString = telegramBotService.getUpdates(lastUpdateId)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        if (response.updates.isEmpty()) continue

        val updates = response.updates.sortedBy { it.updateId }
        lastUpdateId = updates.last().updateId + 1
        updates.forEach { handleUpdate(it, json, trainer, telegramBotService, fileUserDictionary) }
    }
}

private fun handleUpdate(
    update: Update,
    json: Json,
    trainer: LearnWordsTrainer,
    botService: TelegramBotService,
    fileUserDictionary: FileUserDictionary
) {
    val chatId = update.message?.chat?.chatId ?: update.callbackQuery?.message?.chat?.chatId ?: return
    val username = update.message?.from?.username ?: update.callbackQuery?.from?.username ?: "unknown_user"
    fileUserDictionary.insertUser(chatId, username)

    val message = update.message?.text
    val data = update.callbackQuery?.data ?: ""

    if (message == "/start") {
        botService.sendMainMenu(json, chatId)
        println(chatId)
    }
    when {
        CALLBACK_DATA_START_LEARNING_CLICKED == data -> checkNextQuestionAndSend(trainer, botService, json, chatId)
        CALLBACK_DATA_RETURN_CLICKED == data -> botService.sendMainMenu(json, chatId)
        CALLBACK_DATA_RESET_CLICKED == data -> trainer.resetStatistics(chatId)

        CALLBACK_DATA_STATISTICS_CLICKED == data -> {
            val statistics = trainer.getStatistics(chatId)
            botService.sendMessage(
                json,
                chatId,
                STATISTIC_TO_SEND.format(
                    statistics.learnedWordsCount,
                    statistics.totalWordsCount,
                    statistics.learnedWordsPercent
                )
            )
        }

        data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
            val indexOfClicked = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
            val isRightAnswer = trainer.checkAnswer(indexOfClicked, chatId)

            if (isRightAnswer) botService.sendMessage(json, chatId, "Верно!")
            else botService.sendMessage(
                json,
                chatId,
                "Не верно! ${trainer.question?.correctAnswer?.originalWord} - ${trainer.question?.correctAnswer?.translatedWord}"
            )
            checkNextQuestionAndSend(trainer, botService, json, chatId)
        }

    }
}

private fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer?,
    botService: TelegramBotService,
    json: Json,
    chatId: Long
): Question? {
    val question = trainer?.getNextQuestion(chatId)
    if (question == null) {
        botService.sendMessage(json, chatId, "Вы выучили все слова в базе")
    } else {
        botService.sendQuestion(json, chatId, question)
    }
    return question
}

@Serializable
data class Response(
    @SerialName("result")
    val updates: List<Update>
)

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("from")
    val from: From
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("from")
    val from: From
)

@Serializable
data class From(
    @SerialName("username")
    val username: String? = null
)

@Serializable
data class Chat(
    @SerialName("id")
    val chatId: Long,
    @SerialName("username")
    val username: String? = null,
    @SerialName("title")
    val title: String? = null
)
