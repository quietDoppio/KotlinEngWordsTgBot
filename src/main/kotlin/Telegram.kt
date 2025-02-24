import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true){
        Thread.sleep(2000)
        val updates = getUpdates(botToken, updateId)
        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id") + 11
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if(startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId, endUpdateId)
        updateId = updateIdString.toInt() + 1
    }
}

fun getUpdates(botToken: String, updateId: Int): String{
    val botUrl = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(botUrl)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}
