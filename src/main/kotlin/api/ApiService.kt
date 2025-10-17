package api

import bot.Word
import config.TelegramConfig.Api.API_BASE_URL
import config.TelegramConfig.CallbackData.ANSWER_PREFIX
import config.TelegramConfig.Emojis.answerOptionEmojis
import config.TelegramConfig.CallbackData.CallbacksEnum
import kotlinx.serialization.json.Json
import serializableClasses.InlineKeyboardButton
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

abstract class ApiService(
    protected val botToken: String,
    protected val json: Json,
) {
    protected val botUrl = API_BASE_URL + botToken

    protected val client: HttpClient = HttpClient.newBuilder().build()

    private fun makeGetRequest(commandUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(commandUrl)).GET().build()

    private fun makePostRequest(commandUrl: String, jsonBodyString: String): HttpRequest {
        return HttpRequest.newBuilder().uri(URI.create(commandUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBodyString))
            .build()
    }

    protected fun sendGetForString(url: String): String {
        val request = makeGetRequest(url)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    protected fun sendGetForInputStream(url: String): InputStream {
        val request = makeGetRequest(url)
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return response.body()
    }

    protected fun sendPostForString(url: String, jsonBody: String): String {
        val request = makePostRequest(url, jsonBody)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
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

    protected fun makeButtons(vararg callbacks: CallbacksEnum): List<InlineKeyboardButton> {
        val buttonsList = callbacks.map { callbackData ->
            InlineKeyboardButton(callbackData.buttonTitle, callbackData.data)
        }
        return buttonsList
    }

    protected fun makeQuestionButtons(words: List<Word>): List<InlineKeyboardButton> {
        val wordsButtons = words.mapIndexed { index, _ ->
            InlineKeyboardButton(answerOptionEmojis[index], "$ANSWER_PREFIX$index")
        }
        return wordsButtons
    }
}