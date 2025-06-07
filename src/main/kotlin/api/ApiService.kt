package api

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

abstract class ApiService(
    protected val botToken: String,
    protected val json: Json,
) {
    protected val botUrl = config.BotConfig.Telegram.API_BASE_URL + botToken
    protected val client: HttpClient = HttpClient.newBuilder().build()

    protected fun makeGetRequest(commandUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(commandUrl)).GET().build()

    protected fun makePostRequest(commandUrl: String, jsonBodyString: String): HttpRequest {
        return HttpRequest.newBuilder().uri(URI.create(commandUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBodyString))
            .build()
    }

    protected fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>
    ): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when (entry.value) {
                is Path -> {
                    val path = entry.value as Path
                    val mimeType = Files.probeContentType(path)
                    byteArrays.add(
                        "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }

                else -> {
                    byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
                }
            }

        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))
        this.header("Content-type", "multipart/form-data;boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))

        return this
    }
}