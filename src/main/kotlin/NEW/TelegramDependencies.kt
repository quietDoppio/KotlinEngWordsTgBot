package NEW

import bot.Question
import bot.TelegramApiService
import bot.serializableClasses.BotUpdate
import bot.serializableClasses.Response
import kotlinx.serialization.json.Json
import java.io.File

class TelegramUpdateSource(
    private val api: TelegramApiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : UpdateSource {

    override fun getUpdates(lastUpdateId: Long): List<BotUpdate> {
        val responseString = api.getUpdates(lastUpdateId)
        val responseObj = json.decodeFromString<Response>(responseString)
        return responseObj.updates
    }
}

class TelegramFilesHelper(
    private val api: TelegramApiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FilesHelper {
    override fun downloadFile(apiFilePath: String, fileName: String) {
        api.downloadFile(apiFilePath, fileName)
    }

    override fun getFileRequest(fileId: String): String {
        return api.getFileRequest(fileId, json)
    }

}


class TelegramRequestSender(
    private val api: TelegramApiService,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : BotRequestSender {
    override fun sendMessage(chatId: Long, text: String): String {
        return api.sendMessage(json, chatId, text)
    }

    override fun editMessage(chatId: Long, messageId: Long, newText: String): String {
        return api.editMessage(chatId, messageId, newText, json)
    }

    override fun sendMainMenu(chatId: Long): String {
        return api.sendMainMenu(json, chatId)
    }

    override fun sendNewWordsRequest(chatId: Long, text: String): String {
        return api.sendNewWordsRequest(json, chatId, text)
    }

    override fun sendQuestion(chatId: Long, question: Question): String {
        return api.sendQuestion(json, chatId, question)
    }

    override fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean): String {
        return api.sendPhotoByFileId(fileId, chatId, hasSpoiler, json)
    }

    override fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean): String {
        return api.sendPhotoByFile(file, chatId, hasSpoiler)
    }
}