package NEW

import bot.Question
import java.io.File

interface BotRequestSender {
    fun sendMessage(chatId: Long, text: String): String
    fun editMessage(chatId: Long, messageId: Long, newText: String): String
    fun sendMainMenu(chatId: Long): String
    fun sendNewWordsRequest(chatId: Long, text: String): String
    fun sendQuestion(chatId: Long, question: Question): String
    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String
    fun sendPhotoByFile(file: File, chatId: Long, hasSpoiler: Boolean = false): String
}