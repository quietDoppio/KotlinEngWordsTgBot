import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val API_TELEGRAM_URL = "https://api.telegram.org/bot"

class TelegramBotService(botToken: String) {

    private val botUrl = API_TELEGRAM_URL + botToken
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val request: HttpRequest = makeRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Int ,messageToSend: String): String {
            val message = messageToSend.replace(" ", "+")
            val request: HttpRequest = makeRequest("$botUrl/sendMessage?chat_id=$chatId&text=$message")
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            return response.body()
    }

    fun getLastUpdateMatchResult(updates: String, updatesRegex: Regex): MatchResult? =
        updatesRegex.findAll(updates).lastOrNull()

    fun getChatIdFromMatchResult(match: MatchResult?): Int{
        val chatId = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return chatId
    }
    private fun makeRequest(botUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
}