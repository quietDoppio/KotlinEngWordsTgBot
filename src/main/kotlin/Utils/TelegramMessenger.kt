package Utils

import api.TelegramApiService
import bot.Question
import java.io.File

interface TelegramMessenger {
    fun sendMessage(chatId: Long, text: String): String
    fun editMessage(chatId: Long, messageId: Long, newText: String): String
    fun sendNewWordsRequest(chatId: Long, text: String): String
    fun sendQuestion(chatId: Long, question: Question): String
    fun sendMainMenu(chatId: Long): String
    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String
    fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean = false): String
}

class TelegramMessengerImpl(
    private val api: TelegramApiService
) : TelegramMessenger {
    override fun sendMessage(chatId: Long, text: String): String {
        return api.sendMessage(chatId, text)
    }

    override fun editMessage(chatId: Long, messageId: Long, newText: String): String {
        return api.editMessage(chatId, messageId, newText)
    }

    override fun sendMainMenu(chatId: Long): String {
        return api.sendMainMenu(chatId)
    }

    override fun sendNewWordsRequest(chatId: Long, text: String): String {
        return api.sendNewWordsRequest(chatId, text)
    }

    override fun sendQuestion(chatId: Long, question: Question): String {
        return api.sendQuestion(chatId, question)
    }

    override fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean): String {
        return api.sendPhotoByFileId(fileId, chatId, hasSpoiler)
    }

    override fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean): String {
        return api.sendPhotoByFile(file, chatId, hasSpoiler)
    }
}