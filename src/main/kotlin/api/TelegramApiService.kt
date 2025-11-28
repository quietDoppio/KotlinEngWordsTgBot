package api

import bot.Question
import config.BotConfig
import config.TelegramConfig.Api.API_BASE_URL
import config.TelegramConfig.Api.API_DELETE_MESSAGES
import config.TelegramConfig.Api.API_EDIT_MESSAGE_TEXT
import config.TelegramConfig.Api.API_GET_FILE
import config.TelegramConfig.Api.API_GET_UPDATES
import config.TelegramConfig.Api.API_SEND_MESSAGE
import config.TelegramConfig.Api.API_SEND_PHOTO
import config.TelegramConfig.CallbackData.CallbacksEnum
import config.TelegramConfig.Emojis.answerOptionEmojis
import serializableClasses.EditMessageBody
import serializableClasses.GetFileBody
import serializableClasses.ReplyMarkup
import serializableClasses.SendMessageBody
import serializableClasses.SendPhotoBody
import kotlinx.serialization.json.Json
import serializableClasses.BotUpdate
import serializableClasses.DeleteMessageBody
import serializableClasses.TelegramResponse
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Random

class TelegramApiService(botToken: String, json: Json = Json { ignoreUnknownKeys = true }) :
    ApiService(botToken, json) {

    private val returnMenuButton = makeButtons(CallbacksEnum.RETURN_MAIN_MENU_CLICKED)

    fun getUpdates(updateId: Long): List<BotUpdate> {
        val url = "$botUrl$API_GET_UPDATES?offset=$updateId"
        val response = sendGetForString(url)
        val freshUpdates = json.decodeFromString<TelegramResponse<List<BotUpdate>>>(response).result

        val sortedUpdates: List<BotUpdate>? = freshUpdates?.let { updates ->
            updates.sortedBy { it.updateId }
        }

        return sortedUpdates ?: emptyList()
    }

    fun sendMessage(chatId: Long, message: String): String {
        val url = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(chatId, message)
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(url, jsonBodyString)
    }

    fun sendWordEditorMenu(chatId: Long, message: String): String {
        val url = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    makeButtons(CallbacksEnum.EDIT_WORD_SESSION_CLICKED, CallbacksEnum.SET_PICTURE_SESSION_CLICKED),
                    makeButtons(CallbacksEnum.DELETE_WORD_CLICKED, CallbacksEnum.DELETE_HINT_CLICKED),
                    makeButtons(CallbacksEnum.GO_BACK_TO_SELECT_WORD_SESSION_CLICKED) + returnMenuButton
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(url, jsonBodyString)
    }

    fun sendAddPictureMenu(chatId: Long, message: String): String {
        val url = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    makeButtons(CallbacksEnum.GO_BACK_TO_WORD_MENU_CLICKED) + returnMenuButton
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(url, jsonBodyString)
    }

    fun sendShowWordsMenu(chatId: Long, message: String, isWordsEmpty: Boolean = false): String {
        val menuDeleteButton =
            if (isWordsEmpty)
                returnMenuButton
            else
                makeButtons(CallbacksEnum.DELETE_DICTIONARY_CLICKED) + returnMenuButton

        val url = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    makeButtons(CallbacksEnum.ADD_WORDS_SESSION_CLICKED),
                    menuDeleteButton
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(url, jsonBodyString)
    }

    fun sendWordsRequest(chatId: Long, message: String, isAddWordsRequest: Boolean = true): String {
        val goBackButton =
            if (isAddWordsRequest)
                makeButtons(CallbacksEnum.GO_BACK_TO_SELECT_WORD_SESSION_CLICKED)
            else
                makeButtons(CallbacksEnum.GO_BACK_TO_WORD_MENU_CLICKED)


        val url = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(listOf(goBackButton + returnMenuButton))
        )

        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(url, jsonBodyString)
    }

    fun sendMainMenu(chatId: Long): String {
        val sendMessageUrl = "$botUrl$API_SEND_MESSAGE"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    makeButtons(CallbacksEnum.START_LEARNING_CLICKED, CallbacksEnum.SELECT_WORD_SESSION_CLICKED),
                    makeButtons(CallbacksEnum.STATISTICS_CLICKED, CallbacksEnum.RESET_CLICKED),
                )
            )
        )

        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(sendMessageUrl, jsonBodyString)
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val sendMessageUrl = "$botUrl$API_SEND_MESSAGE"

        val words = question.variants

        val questionString: String = buildString {
            append("${question.correctAnswer.originalWord}\n")
            append(words.mapIndexed { index, word ->
                "\n${answerOptionEmojis[index]} ${word.translatedWord}"
            }.joinToString("\n"))
        }

        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = questionString,
            replyMarkup = ReplyMarkup(listOf(makeQuestionButtons(words), returnMenuButton)),
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostForString(sendMessageUrl, jsonBodyString)
    }

    fun deleteMessages(chatId: Long, messageIds: List<Long>): String {
        val deleteMessageUrl = "$botUrl$API_DELETE_MESSAGES"
        val jsonBody = json.encodeToString(DeleteMessageBody(chatId, messageIds))

        return sendPostForString(deleteMessageUrl, jsonBody)
    }

    fun editWordRequest(chatId: Long, messageId: Long, message: String, isAddWordsRequest: Boolean = true): String {
        val editMessageUrl = "$botUrl$API_EDIT_MESSAGE_TEXT"

        val goBackButton =
            if (isAddWordsRequest)
                makeButtons(CallbacksEnum.GO_BACK_TO_SELECT_WORD_SESSION_CLICKED)
            else
                makeButtons(CallbacksEnum.GO_BACK_TO_WORD_MENU_CLICKED)

        val jsonBody = json.encodeToString(
            EditMessageBody(
                chatId = chatId,
                messageId = messageId,
                text = message,
                replyMarkup = ReplyMarkup(listOf(goBackButton + returnMenuButton))
            )
        )

        return sendPostForString(editMessageUrl, jsonBody)
    }

    fun downloadFile(apiFilePath: String, fileName: String) {
        val newFile = File(fileName)

        val urlFilePath = "${API_BASE_URL.dropLast(4)}/file/bot$botToken/$apiFilePath"
        val response: InputStream = sendGetForInputStream(urlFilePath)

        response.use { body ->
            newFile.outputStream().use { fileStream ->
                body.copyTo(fileStream, BotConfig.Files.COPY_BUFFER_SIZE)
            }
        }
    }

    fun getFileRequest(fileId: String): String {
        val urlGetFile = "$botUrl$API_GET_FILE"
        val requestBody = GetFileBody(fileId)
        val requestBodyString = json.encodeToString(requestBody)

        return sendPostForString(urlGetFile, requestBodyString)
    }

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String {
        val getFileBody = SendPhotoBody(chatId, fileId, hasSpoiler)
        val jsonBody = json.encodeToString(getFileBody)

        return sendPostForString("$botUrl$API_SEND_PHOTO", jsonBody)
    }

    fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file.toPath()
        data["has_spoiler"] = hasSpoiler
        val boundary = BigInteger(35, Random()).toString()

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$botUrl$API_SEND_PHOTO"))
            .postMultipartFormData(boundary, data)
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}