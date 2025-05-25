import dictionary.Question
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random

const val API_TELEGRAM_URL = "https://api.telegram.org/bot"
const val CALLBACK_DATA_STATISTICS_CLICKED = "DATA_CALLBACK_STATISTICS_CLICKED"
const val CALLBACK_DATA_START_LEARNING_CLICKED = "DATA_CALLBACK_START_LEARNING_CLICKED"
const val CALLBACK_DATA_RESET_CLICKED = "CALLBACK_DATA_RESET_CLICKED"
const val CALLBACK_DATA_RETURN_CLICKED = "CALLBACK_DATA_RETURN_CLICKED"
const val CALLBACK_DATA_ADD_WORDS = "CALLBACK_DATA_ADD_WORDS"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val EMOJI_DIGITS = "\uFE0F\u20E3"

class TelegramBotService(private val botToken: String) {

    private val numsEmojiList = ('1'..'4').map { it -> "$it$EMOJI_DIGITS" }
    private val botUrl = API_TELEGRAM_URL + botToken
    private val client: HttpClient = HttpClient.newBuilder().build()


    fun getUpdates(updateId: Long): String {
        val request: HttpRequest = makeGetRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(json: Json, chatId: Long, message: String): String {
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
        )
        val jsonBodyString = json.encodeToString(jsonBody)

        val request: HttpRequest = makePostRequest(
            botUrl = "${botUrl}/sendMessage",
            jsonBodyString = jsonBodyString,
        )
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendNewWordsRequest(json: Json, chatId: Long, message: String): String {
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            text = "меню",
                            callbackData = CALLBACK_DATA_RETURN_CLICKED,
                        )
                    )
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)

        val request: HttpRequest = makePostRequest(
            botUrl = "${botUrl}/sendMessage",
            jsonBodyString = jsonBodyString,
        )
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMainMenu(json: Json, chatId: Long): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            text = "Изучение слов",
                            callbackData = CALLBACK_DATA_START_LEARNING_CLICKED
                        ),
                        InlineKeyboardButton(text = "Статистика", callbackData = CALLBACK_DATA_STATISTICS_CLICKED)
                    ),
                    listOf(
                        InlineKeyboardButton(text = "Обновить словарь", callbackData = CALLBACK_DATA_ADD_WORDS),
                        InlineKeyboardButton(text = "Сбросить статистику", callbackData = CALLBACK_DATA_RESET_CLICKED),
                    )
                )
            )
        )

        val jsonBodyString = json.encodeToString(jsonBody)
        val postJsonRequest: HttpRequest = makePostRequest(sendMessageUrl, jsonBodyString)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val questionString: String = buildString {
            append("${question.correctAnswer.originalWord}\n")
            append(question.variants.mapIndexed { index, word ->
                "\n${numsEmojiList[index]} ${word.translatedWord}"
            }.joinToString("\n"))
        }
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = questionString,
            replyMarkup = ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, word ->
                        InlineKeyboardButton(
                            text = numsEmojiList[index],
                            callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                        )
                    },
                    listOf(
                        InlineKeyboardButton(
                            text = "Вернуться в меню",
                            callbackData = CALLBACK_DATA_RETURN_CLICKED
                        )
                    )
                )
            ),
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        val postJsonRequest: HttpRequest = makePostRequest(sendMessageUrl, jsonBodyString)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun editMessage(chatId: Long, messageId: Long, message: String, json: Json): String {
        val editMessageUrl = "$botUrl/editMessageText"
        val jsonBody = json.encodeToString(
            EditMessageBody(
                chatId = chatId,
                messageId = messageId,
                text = message,
                replyMarkup = ReplyMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                text = "меню",
                                callbackData = CALLBACK_DATA_RETURN_CLICKED,
                            )
                        )
                    )
                )

            )
        )
        val request: HttpRequest = makePostRequest(editMessageUrl, jsonBody)
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val urlFilePath = "${API_TELEGRAM_URL.dropLast(4)}/file/bot$botToken/$filePath"
        println(urlFilePath)
        val request: HttpRequest = makeGetRequest(urlFilePath)
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        println("Status code: ${response.statusCode()}")
        val body: InputStream = response.body()
        val newFile = File(fileName)
        body.copyTo(newFile.outputStream(), 16 * 1024)
    }

    fun getFileRequest(fileId: String, json: Json): String {
        val urlGetFile = "$botUrl/getFile"
        val requestBody = GetFileBody(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val request = makePostRequest(urlGetFile, requestBodyString)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false, json: Json): String {
        val getFileBody = SendPhotoBody(chatId = chatId, fileId = fileId, hasSpoiler = hasSpoiler)
        val jsonBody = json.encodeToString(getFileBody)

        val request = makePostRequest("$botUrl/sendPhoto", jsonBody)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file.toPath()
        data["has_spoiler"] = hasSpoiler
        val boundary = BigInteger(35, Random()).toString()

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$botUrl/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun HttpRequest.Builder.postMultipartFormData(boundary: String, data: Map<String, Any>): HttpRequest.Builder {
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

    private fun makeGetRequest(botUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(botUrl)).GET().build()

    private fun makePostRequest(botUrl: String, jsonBodyString: String): HttpRequest {
        return HttpRequest.newBuilder().uri(URI.create(botUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBodyString))
            .build()
    }

    @Serializable
    data class SendMessageBody(
        @SerialName("chat_id")
        val chatId: Long,
        @SerialName("text")
        val text: String,
        @SerialName("reply_markup")
        val replyMarkup: ReplyMarkup? = null,
    )

    @Serializable
    data class EditMessageBody(
        @SerialName("chat_id")
        val chatId: Long,
        @SerialName("message_id")
        val messageId: Long,
        @SerialName("text")
        val text: String,
        @SerialName("reply_markup")
        val replyMarkup: ReplyMarkup
    )

    @Serializable
    data class GetFileBody(
        @SerialName("file_id")
        val fileId: String,
    )

    @Serializable
    data class SendPhotoBody(
        @SerialName("chat_id")
        val chatId: Long?,
        @SerialName("photo")
        val fileId: String,
        @SerialName("has_spoiler")
        val hasSpoiler: Boolean
    )

    @Serializable
    data class ReplyMarkup(
        @SerialName("inline_keyboard")
        val inlineKeyboard: List<List<InlineKeyboardButton>>
    )

    @Serializable
    data class InlineKeyboardButton(
        @SerialName("text")
        val text: String,
        @SerialName("callback_data")
        val callbackData: String
    )
}