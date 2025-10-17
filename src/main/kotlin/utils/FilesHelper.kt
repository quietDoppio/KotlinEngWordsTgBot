package utils

import api.TelegramApiService
import bot.Word
import database.DataBaseRepository
import java.io.File
import kotlinx.serialization.json.Json
import serializableClasses.Document
import serializableClasses.TelegramFile
import serializableClasses.TelegramResponse
import utils.TextChecker.isTextCyrillic
import utils.TextChecker.isTextLatin

class FilesHelper(
        private val api: TelegramApiService,
        private val repository: DataBaseRepository,
        private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun addWordsFromFile(document: Document?, chatId: Long): Boolean {
        if (document == null || !document.fileName.endsWith(".txt")) {
            println("Файл не найден или не того формата")
            return false
        }

        val textFile: File = downloadDocument(document) ?: return false
        val newWords = parseWordsFromFile(textFile)

        val isWordAdded =
                if (newWords.isNotEmpty()) {
                    saveWordsToRepository(chatId, newWords)
                    textFile.delete()
                    true
                } else {
                    false
                }

        return isWordAdded
    }

    private fun downloadDocument(document: Document): File? {
        val jsonResponse = api.getFileRequest(document.fileId)
        val response = json.decodeFromString<TelegramResponse<TelegramFile>>(jsonResponse)
        response.result?.filePath?.let { filePath -> api.downloadFile(filePath, document.fileName) }
        val textFile = File(document.fileName)

        return if (textFile.exists()) textFile else null
    }

    private fun parseWordsFromFile(textFile: File): List<Word> {
        val newWords: MutableList<Word> = mutableListOf()
        textFile.forEachLine { line ->
            val word = parseWordFromLine(line)
            if (word != null) newWords.add(word)
        }
        return newWords
    }

    private fun parseWordFromLine(line: String): Word? {
        val parts = line.split("|")
        if (parts.size != 2) return null

        val (original, translated) = parts.map { it.trim() }
        if (!original.isTextLatin() || !translated.isTextCyrillic()) return null

        return Word(originalWord = original, translatedWord = translated)
    }

    private fun saveWordsToRepository(chatId: Long, words: List<Word>) {
        if (words.isNotEmpty()) {
            repository.addWords(chatId, words)
            repository.addUserAnswersToUser(chatId, words)
        }
    }
}
