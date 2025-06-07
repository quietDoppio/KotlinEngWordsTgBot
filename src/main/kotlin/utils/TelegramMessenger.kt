package utils

import api.TelegramApiService
import bot.Question
import java.io.File

class TelegramMessenger(
    private val api: TelegramApiService
) {
    fun sendMessage(chatId: Long, text: String): String =
        api.sendMessage(chatId, text)

    fun sendAddPictureMenu(chatId: Long, text: String): String =
        api.sendAddPictureMenu(chatId, text)

    fun deleteMessages(chatId: Long, messageIds: List<Long>): String =
        api.deleteMessages(chatId, messageIds)

    fun editWordRequest(chatId: Long, messageId: Long, newText: String, isAddWordsRequest: Boolean = true): String =
        api.editWordRequest(chatId, messageId, newText, isAddWordsRequest)

    fun sendMainMenu(chatId: Long): String =
        api.sendMainMenu(chatId)

    fun sendShowWordsMenu(chatId: Long, text: String, isWordsEmpty: Boolean = false): String =
        api.sendShowWordsMenu(chatId, text, isWordsEmpty)

    fun sendWordMenu(chatId: Long, text: String) =
        api.sendWordMenu(chatId, text)

    fun sendWordsRequest(chatId: Long, text: String, isAddWordsRequest: Boolean = true): String =
        api.sendWordsRequest(chatId, text, isAddWordsRequest)

    fun sendQuestion(chatId: Long, question: Question): String =
        api.sendQuestion(chatId, question)

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean): String =
        api.sendPhotoByFileId(fileId, chatId, hasSpoiler)

    fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean): String =
        api.sendPhotoByFile(file, chatId, hasSpoiler)

}