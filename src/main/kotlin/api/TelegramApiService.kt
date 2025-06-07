package api

import bot.Question
import config.BotConfig
import config.BotConfig.Messages.GO_BACK_MESSAGE
import config.BotConfig.Telegram.Callbacks
import config.BotConfig.Telegram.Callbacks.ADD_WORDS_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.ANSWER_PREFIX
import config.BotConfig.Telegram.Callbacks.DELETE_DICTIONARY_CLICKED
import config.BotConfig.Telegram.Callbacks.DELETE_WORD_CLICKED
import config.BotConfig.Telegram.Callbacks.EDIT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.GO_BACK_SELECT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.GO_BACK_WORD_MENU_CLICKED
import config.BotConfig.Telegram.Callbacks.RESET_CLICKED
import config.BotConfig.Telegram.Callbacks.RETURN_MAIN_MENU_CLICKED
import config.BotConfig.Telegram.Callbacks.SELECT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.SET_PICTURE_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.START_LEARNING_CLICKED
import config.BotConfig.Telegram.Callbacks.STATISTICS_CLICKED
import config.BotConfig.Telegram.Emojis
import serializableClasses.EditMessageBody
import serializableClasses.GetFileBody
import serializableClasses.InlineKeyboardButton
import serializableClasses.ReplyMarkup
import serializableClasses.SendMessageBody
import serializableClasses.SendPhotoBody
import kotlinx.serialization.json.Json
import serializableClasses.CopyMessageBody
import serializableClasses.DeleteMessageBody
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Random

class TelegramApiService(botToken: String, json: Json = Json { ignoreUnknownKeys = true }) :
    ApiService(botToken, json) {

    private val answerOptionEmojis = ('1'..'4').map { it -> "$it${Emojis.DIGITS}" }
    private val menuButton = InlineKeyboardButton(
        text = "Меню",
        callbackData = RETURN_MAIN_MENU_CLICKED,
    )

    fun getUpdates(updateId: Long): String {
        val request: HttpRequest = makeGetRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, message: String): String {
        val url = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(chatId = chatId, text = message)
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(url, jsonBodyString)
    }

    fun copyMessage(chatId: Long, messageId: Long): String {
        val url = "${botUrl}/copyMessage"
        val jsonBody = CopyMessageBody(
            chatId = chatId,
            fromChatId = chatId,
            messageId = messageId
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        val copyMessageResponse = sendPostRequest(url, jsonBodyString)
        println(copyMessageResponse)
        return copyMessageResponse
    }

    fun sendWordMenu(chatId: Long, message: String): String {
        val url = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton("Редактировать", EDIT_WORD_SESSION_CLICKED),
                        InlineKeyboardButton("Установить картинку-подсказку", SET_PICTURE_SESSION_CLICKED),
                    ),
                    listOf(
                        InlineKeyboardButton("Удалить", DELETE_WORD_CLICKED),
                        InlineKeyboardButton(GO_BACK_MESSAGE, GO_BACK_SELECT_WORD_SESSION_CLICKED)
                    ),
                    listOf(menuButton)
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(url, jsonBodyString)
    }

    fun sendAddPictureMenu(chatId: Long, message: String): String {
        val url = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(GO_BACK_MESSAGE, GO_BACK_WORD_MENU_CLICKED),
                        menuButton
                    )
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(url, jsonBodyString)
    }

    fun sendShowWordsMenu(chatId: Long, message: String, isWordsEmpty: Boolean): String {
        val menuDeleteButton = if (isWordsEmpty) {
            listOf(menuButton)
        } else {
            listOf(InlineKeyboardButton("Удалить всё", DELETE_DICTIONARY_CLICKED), menuButton)
        }

        val url = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(InlineKeyboardButton("Добавить слова", ADD_WORDS_SESSION_CLICKED)), menuDeleteButton
                )
            )
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(url, jsonBodyString)
    }

    fun sendWordsRequest(chatId: Long, message: String, isAddWordsRequest: Boolean): String {
        val goBackButton = InlineKeyboardButton(
            GO_BACK_MESSAGE,
            if(isAddWordsRequest) GO_BACK_SELECT_WORD_SESSION_CLICKED else GO_BACK_WORD_MENU_CLICKED
        )
        val url = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = message,
            replyMarkup = ReplyMarkup(listOf(listOf(goBackButton, menuButton))))
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(url, jsonBodyString)
    }

    fun sendMainMenu(chatId: Long): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton("Изучение слов", START_LEARNING_CLICKED),
                        InlineKeyboardButton("Показать словарь", SELECT_WORD_SESSION_CLICKED),
                    ),
                    listOf(
                        InlineKeyboardButton("Статистика", STATISTICS_CLICKED),
                        InlineKeyboardButton("Сбросить статистику", RESET_CLICKED),
                    ),
                )
            )
        )

        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(sendMessageUrl, jsonBodyString)
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val questionString: String = buildString {
            append("${question.correctAnswer.originalWord}\n")
            append(question.variants.mapIndexed { index, word ->
                "\n${answerOptionEmojis[index]} ${word.translatedWord}"
            }.joinToString("\n"))
        }
        val jsonBody = SendMessageBody(
            chatId = chatId,
            text = questionString,
            replyMarkup = ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, word ->
                        InlineKeyboardButton(answerOptionEmojis[index], "${ANSWER_PREFIX}$index")
                    },
                    listOf(InlineKeyboardButton("Вернуться в меню", RETURN_MAIN_MENU_CLICKED))
                )
            ),
        )
        val jsonBodyString = json.encodeToString(jsonBody)
        return sendPostRequest(sendMessageUrl, jsonBodyString)
    }

    fun deleteMessages(chatId: Long, messageIds: List<Long>): String {
        val deleteMessageUrl = "$botUrl/deleteMessages"
        val jsonBody = json.encodeToString(DeleteMessageBody(chatId = chatId, messageIds = messageIds))

        return sendPostRequest(deleteMessageUrl, jsonBody)
    }

    fun editWordRequest(chatId: Long, messageId: Long, message: String, isAddWordsRequest: Boolean): String {
        val goBackButton = InlineKeyboardButton(
            GO_BACK_MESSAGE,
            if(isAddWordsRequest) GO_BACK_SELECT_WORD_SESSION_CLICKED else GO_BACK_WORD_MENU_CLICKED
        )
        val editMessageUrl = "$botUrl/editMessageText"
        val jsonBody = json.encodeToString(
            EditMessageBody(
                chatId = chatId,
                messageId = messageId,
                text = message,
                replyMarkup = ReplyMarkup(listOf(listOf(goBackButton, menuButton)))
            )
        )
        return sendPostRequest(editMessageUrl, jsonBody)
    }

    fun downloadFile(apiFilePath: String, fileName: String) {
        val urlFilePath = "${BotConfig.Telegram.API_BASE_URL.dropLast(4)}/file/bot$botToken/$apiFilePath"

        val request: HttpRequest = makeGetRequest(urlFilePath)
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val newFile = File(fileName)
        response.body().use { body ->
            newFile.outputStream().use { fileStream ->
                body.copyTo(fileStream, BotConfig.Files.COPY_BUFFER_SIZE)
            }
        }

    }

    fun getFileRequest(fileId: String): String {
        val urlGetFile = "$botUrl/getFile"
        val requestBody = GetFileBody(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)

        return sendPostRequest(urlGetFile, requestBodyString)
    }

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String {
        val getFileBody = SendPhotoBody(chatId = chatId, fileId = fileId, hasSpoiler = hasSpoiler)
        val jsonBody = json.encodeToString(getFileBody)

        return sendPostRequest("$botUrl/sendPhoto", jsonBody)
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

    fun sendPostRequest(url: String, jsonBody: String): String {
        val request = makePostRequest(url, jsonBody)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}