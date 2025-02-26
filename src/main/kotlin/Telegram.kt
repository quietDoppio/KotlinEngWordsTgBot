import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken: String = args[0]
    var updates: String
    var lastUpdateMatch: MatchResult?

    val updatesRegex: Regex = "\"update_id\":(\\d+).*?\"text\":\"(.+?)\"".toRegex(RegexOption.DOT_MATCHES_ALL)

    var updateId = 0
    var message: String

    while (true){
        Thread.sleep(2000)
        updates = getUpdates(botToken, updateId)

        lastUpdateMatch = updatesRegex.findAll(updates).lastOrNull()?.also {
            updateId = it.groupValues[1].toInt() + 1
            message = it.groupValues[2]
            println(message)
        }
    }
}

fun getUpdates(botToken: String, updateId: Int): String{
    val botUrl = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}
