import dictionary.LearnWordsTrainer
import dictionary.Question
import dictionary.STATISTIC_TO_SEND
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainers = mutableListOf<LearnWordsTrainer>()
    val uniqueTrainersIds = mutableSetOf<Long>()

    val json = Json {
        ignoreUnknownKeys = true
    }
    var responseString: String
    var lastUpdate: Update
    var lastUpdateId = 0L
    var chatId = 0L
    var message: String?
    var data: String?
    var question: Question? = null



    while (true) {
        Thread.sleep(2000)
        responseString = telegramBotService.getUpdates(lastUpdateId)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        val updates = response.result

        lastUpdate = updates.lastOrNull() ?: continue
        lastUpdateId = lastUpdate.updateId + 1
        chatId = lastUpdate.message?.chat?.chatId ?: lastUpdate.callbackQuery?.message?.chat?.chatId ?: 0
        message = lastUpdate.message?.text
        data = lastUpdate.callbackQuery?.data

        if (uniqueTrainersIds.add(chatId)) trainers.add(LearnWordsTrainer(id = chatId))
        if (message == "/menu") {
            telegramBotService.sendMainMenu(json, chatId)
            println(chatId)
        }

        data?.let { data ->
            val currentTrainer = trainers.find { it.id == chatId }
            when {
                CALLBACK_DATA_START_LEARNING_CLICKED == data ->
                    question = checkNextQuestionAndSend(
                        currentTrainer,
                        telegramBotService,
                        json,
                        chatId
                    )

                CALLBACK_DATA_STATISTICS_CLICKED == data -> {
                    val statistics = currentTrainer?.getStatistics()
                    telegramBotService.sendMessage(
                        json,
                        chatId,
                        STATISTIC_TO_SEND.format(
                            statistics?.learnedWordsCount,
                            statistics?.totalWordsCount,
                            statistics?.learnedWordsPercent
                        )
                    )
                }

                data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    val correctAnswer = question?.correctAnswer
                    val indexOfClicked = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
                    val isRightAnswer = currentTrainer?.checkAnswer(indexOfClicked) ?: false

                    if (isRightAnswer) telegramBotService.sendMessage(json,chatId, "Верно!")
                    else telegramBotService.sendMessage(
                        json,
                        chatId,
                        "Не верно! ${correctAnswer?.originalWord} - ${correctAnswer?.translatedWord}"
                    )
                    question = checkNextQuestionAndSend(currentTrainer, telegramBotService, json, chatId)
                }

                else -> ""
            }
        }
    }

}

private fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer?,
    telegramBotService: TelegramBotService,
    json: Json,
    chatId: Long
): Question? {
    val question = trainer?.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(json ,chatId, "Вы выучили все слова в базе")
    } else {
        telegramBotService.sendQuestion(json, chatId, question)
    }
    return question
}

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>
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
    val message: Message? = null
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat
)

@Serializable
data class Chat(
    @SerialName("id")
    val chatId: Long,
)
