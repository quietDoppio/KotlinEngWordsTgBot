import dictionary.Question
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val API_TELEGRAM_URL = "https://api.telegram.org/bot"
const val CALLBACK_DATA_STATISTICS_CLICKED = "DATA_CALLBACK_STATISTICS"
const val CALLBACK_DATA_START_LEARNING_CLICKED = "DATA_CALLBACK_START_LEARNING"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(botToken: String) {

    private val botUrl = API_TELEGRAM_URL + botToken
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val request: HttpRequest = makeRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(json: Json, chatId: Long, message: String): String {
        val jsonBody = SendMessageRequest(
            chatId = chatId,
            text = message,
        )
        val jsonBodyString = json.encodeToString(jsonBody)

        val request: HttpRequest = makePostJsonRequest(
            botUrl = "${botUrl}/sendMessage",
            jsonBodyString = jsonBodyString,
        )
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMainMenu(json: Json, chatId: Long): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val jsonBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "Изучение слов", callbackData = CALLBACK_DATA_START_LEARNING_CLICKED),
                        InlineKeyboard(text = "Статистика", callbackData = CALLBACK_DATA_STATISTICS_CLICKED)
                    )
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        val postJsonRequest: HttpRequest = makePostJsonRequest(sendMessageUrl, jsonBodyString)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val jsonBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.originalWord,
            replyMarkup = ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, word ->
                        InlineKeyboard(
                            text = word.translatedWord,
                            callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                        )
                    }
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        val postJsonRequest: HttpRequest = makePostJsonRequest(sendMessageUrl, jsonBodyString)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    private fun makeRequest(botUrl: String): HttpRequest = HttpRequest.newBuilder().uri(URI.create(botUrl)).build()

    private fun makePostJsonRequest(botUrl: String, jsonBodyString: String): HttpRequest {
        return HttpRequest.newBuilder().uri(URI.create(botUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBodyString))
            .build()
    }
}

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>
)

@Serializable
data class InlineKeyboard(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String
)