import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
const val GET_ME = "getMe"
const val GET_UPDATES = "getUpdates"
fun main(args: Array<String>) {
    val botToken = args[0]
    val botUrl = "https://api.telegram.org/bot$botToken/%s"

    val client: HttpClient = HttpClient.newBuilder().build()
    val getMeRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(botUrl.format(GET_ME)))
        .build()
    val getUpdatesRequest: HttpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(botUrl.format(GET_UPDATES)))
        .build()

    val getMeResponse: HttpResponse<String> = client.send(getMeRequest, HttpResponse.BodyHandlers.ofString())
    val getUpdatesResponse: HttpResponse<String> = client.send(getUpdatesRequest, HttpResponse.BodyHandlers.ofString())

    println(getMeResponse.body())
    println(getUpdatesResponse.body())
}
