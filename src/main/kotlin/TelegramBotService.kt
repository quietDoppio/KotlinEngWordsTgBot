import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val API_TELEGRAM_URL = "https://api.telegram.org/bot"
const val DATA_CALLBACK_STATISTICS = "DATA_CALLBACK_STATISTICS"
const val DATA_CALLBACK_START_LEARNING = "DATA_CALLBACK_START_LEARNING"

class TelegramBotService(botToken: String) {

    private val botUrl = API_TELEGRAM_URL + botToken
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val request: HttpRequest = makeRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, messageToSend: String): String {
        val encoded = URLEncoder.encode(messageToSend, StandardCharsets.UTF_8)
        val message = encoded.replace(" ", "+")
        val request: HttpRequest = makeRequest("$botUrl/sendMessage?chat_id=$chatId&text=$message")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMainMenu(chatId: Long): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val menuJsonBody = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Начать изучение",
                      "callback_data": "$DATA_CALLBACK_START_LEARNING"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "$DATA_CALLBACK_STATISTICS"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()
        val postJsonRequest: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessageUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(menuJsonBody))
            .build()
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun makeRequest(botUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
}