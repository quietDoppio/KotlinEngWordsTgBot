import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    var message: String

    while (true){
        Thread.sleep(2000)
        val updates = getUpdates(botToken, updateId)

        val updateRegex = "\"update_id\":(\\d+).*?\"text\":\"(.+?)\"".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = updateRegex.findAll(updates)
        val lastMatch = matches.lastOrNull()?.also {
            updateId = it.groupValues[1].toInt() + 1
            message = it.groupValues[2]
            println(message)
            if (message == "/start") sendMessage(botToken, updates )
        }
    }
}
fun sendMessage(botToken: String, updates: String): String{
    val updatesRegex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val match = updatesRegex.find(updates)
    val chatId = match?.groups?.get(1)?.value?.toInt()

    val botUrl = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=Привет!+бот+ещё+не+работает!"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}
fun getUpdates(botToken: String, updateId: Int): String{
    val botUrl = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}
