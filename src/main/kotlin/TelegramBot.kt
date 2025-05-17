import dictionary.LearnWordsTrainer
import dictionary.Question
import dictionary.STATISTIC_TO_SEND
import dictionary.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

const val CHECKMARK = "\u2705"
const val CROSSMARK = "\u274C"
const val HUNDRED_EMOJI = "\uD83D\uDCAF"
const val ENG_EMOJI = "\uD83C\uDDFA\uD83C\uDDF8"
const val RU_EMOJI = "\uD83C\uDDF7\uD83C\uDDFA"

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()
    val wordSessionState = mutableMapOf<Long, WordSessionState?>()

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
        updates.forEach { handleUpdate(it, json, trainer, telegramBotService, wordSessionState) }
    }
}

private fun handleUpdate(
    update: Update,
    json: Json,
    trainer: LearnWordsTrainer,
    botService: TelegramBotService,
    wordSessionStateMap: MutableMap<Long, WordSessionState?>
) {
    val chatId = update.message?.chat?.chatId ?: update.callbackQuery?.message?.chat?.chatId ?: return
    val message = update.message?.text
    val document = update.message?.document
    val data = update.callbackQuery?.data ?: ""
    val session = wordSessionStateMap[chatId]
    val username = update.message?.from?.username ?: update.callbackQuery?.from?.username ?: "unknown_user"
    trainer.fileUserDictionary.insertUser(chatId, username)

    if (session != null && data != CALLBACK_DATA_RETURN_CLICKED) {
        when (session.waitingFor) {
            WaitingFor.ORIGINAL -> {
                session.currentOriginal = message ?: ""
                session.waitingFor = WaitingFor.TRANSLATION
                botService.sendNewWordsRequest(json, chatId, "Введите перевод $RU_EMOJI:")
            }

            WaitingFor.TRANSLATION -> {
                session.waitingFor = WaitingFor.ORIGINAL

                val original = session.currentOriginal
                val translation = message ?: ""
                val wordToAdd = Word(originalWord = original, translatedWord = translation)
                trainer.fileUserDictionary.updateDictionary(listOf(wordToAdd), chatId)

                botService.sendNewWordsRequest(json, chatId, "Введите оригинал $ENG_EMOJI:")
            }
        }
        return
    }

    if (message == "/start") {
        botService.sendMainMenu(json, chatId)
        println(chatId)
    }

    if (document != null) {
        val jsonResponse = botService.getFileRequest(document.fileId, json)
        val response: GetFileResponse = json.decodeFromString(jsonResponse)
        response.result?.let { response ->
            botService.downloadFile(response.filePath, document.fileName)
        }
        println(response)
        val newWords: MutableList<Word> = mutableListOf()
        File(document.fileName).forEachLine { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                val text = parts[0].trim()
                val translate = parts[1].trim()
                newWords.add(Word(originalWord = text, translatedWord = translate))
                val endsWithNewLine: Boolean = File("words.txt").readText().endsWith("\n")
                val lineToAppend = "${text}|${translate}"
                val correctLine = if (endsWithNewLine) lineToAppend else "\n$lineToAppend"
                File("words.txt").appendText(correctLine)
            }
        }
        trainer.fileUserDictionary.updateDictionary(newWords, chatId)

    }

    when {
        CALLBACK_DATA_START_LEARNING_CLICKED == data -> checkNextQuestionAndSend(trainer, botService, json, chatId)
        CALLBACK_DATA_RETURN_CLICKED == data -> {
            botService.sendMainMenu(json, chatId)
            if (wordSessionStateMap[chatId] != null) wordSessionStateMap[chatId] = null
        }

        CALLBACK_DATA_RESET_CLICKED == data -> {
            trainer.resetStatistics(chatId)
            botService.sendMessage(json, chatId, "Статистика успешно сброшена")
            botService.sendMainMenu(json,chatId)
        }

        CALLBACK_DATA_ADD_WORDS == data -> {
            botService.sendNewWordsRequest(json, chatId, "Введите оригинал $ENG_EMOJI:")
            wordSessionStateMap[chatId] = WordSessionState()
        }

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

            if (isRightAnswer) botService.sendMessage(json, chatId, "$CHECKMARK Верно!")
            else botService.sendMessage(
                json,
                chatId,
                "$CROSSMARK Не верно! ${trainer.question?.correctAnswer?.originalWord} - ${trainer.question?.correctAnswer?.translatedWord}"
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
        botService.sendMessage(json, chatId, "$HUNDRED_EMOJI Вы выучили все слова в базе или они отсутствуют")
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
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("from")
    val from: From,
    @SerialName("document")
    val document: Document? = null
)

@Serializable
data class GetFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String
)

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long
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

data class WordSessionState(
    var waitingFor: WaitingFor = WaitingFor.ORIGINAL,
    var currentOriginal: String = ""
)

enum class WaitingFor {
    ORIGINAL,
    TRANSLATION
}
