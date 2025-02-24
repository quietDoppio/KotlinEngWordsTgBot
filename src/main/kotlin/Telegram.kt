import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    val botToken = args[0]
    val botUrl = "https://api.telegram.org/bot$botToken/%s"

    val client: HttpClient = HttpClient.newBuilder().build()
    val getMeRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(botUrl.format("getMe")))
        .build()
    val getUpdatesRequest: HttpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(botUrl.format("getUpdates")))
        .build()

    val getMeResponse: HttpResponse<String> = client.send(getMeRequest, HttpResponse.BodyHandlers.ofString())
    val getUpdatesResponse: HttpResponse<String> = client.send(getUpdatesRequest, HttpResponse.BodyHandlers.ofString())

    println(getMeResponse.body())
    println(getUpdatesResponse.body())
}
