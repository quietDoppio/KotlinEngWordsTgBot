package utils

import api.TelegramApiService
import bot.LearnWordsTrainer
import bot.Word
import config.BotConfig
import kotlinx.serialization.json.Json
import serializableClasses.Document
import serializableClasses.GetFileResponse
import serializableClasses.PhotoSize
import java.io.File
import kotlin.collections.forEach

class FilesHelper(
    private val api: TelegramApiService,
    private val trainer: LearnWordsTrainer,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun downloadFile(apiFilePath: String, fileName: String) {
        api.downloadFile(apiFilePath, fileName)
    }

    fun getFileRequest(fileId: String): String {
        return api.getFileRequest(fileId)
    }

    fun saveWordsToRepository(chatId: Long, words: List<Word>) {
        if (words.isNotEmpty()) {
            trainer.userRepository.addWords(chatId, words)
            trainer.userRepository.addUserAnswersToUser(chatId, words)
        }
    }

    fun editWordInRepository(chatId: Long, oldWord: Word, newWord: Word) {
        trainer.userRepository.editWord(chatId, oldWord, newWord)
    }

    fun saveWordsToRepository(chatId: Long, word: Word) {
        trainer.userRepository.addWords(chatId, word)
    }

    fun appendWordsToFile(words: List<Word>) {
        val wordsFile = File(BotConfig.Files.WORDS_FILE)
        if (!wordsFile.exists()) wordsFile.createNewFile()
        val wordsLines = wordsFile.readLines()

        for (word in words) {
            val lineToAppend = "${word.originalWord}|${word.translatedWord}"
            if (wordsLines.contains(lineToAppend)) continue

            val endsWithNewLine = wordsFile.readText().endsWith("\n")
            val correctLine = if (endsWithNewLine) lineToAppend else "\n$lineToAppend"
            wordsFile.appendText(correctLine)
        }
    }

    fun parseWordFromLine(line: String): Word? {
        val parts = line.split("|")
        if (parts.size == 2) {
            val text = parts[0].trim()
            val translate = parts[1].trim()
            return Word(originalWord = text, translatedWord = translate)
        }
        return null
    }

    fun parseWordsFromFile(textFile: File): List<Word> {
        val newWords = mutableListOf<Word>()
        textFile.forEachLine { line ->
            val word = parseWordFromLine(line)
            if (word != null) {
                newWords.add(word)
            }
        }
        return newWords
    }

    fun addWordsFromFile(document: Document?, chatId: Long): Boolean {
        if (document == null || !document.fileName.endsWith(".txt")) {
            println("Файл не найден или не того формата")
            return false
        }

        val textFile = downloadDocument(document)
        val isCompleted: Boolean? = textFile?.let { file ->
            val newWords = parseWordsFromFile(file)
            appendWordsToFile(newWords)
            saveWordsToRepository(chatId, newWords)

            file.delete()
            true
        }
        return isCompleted == true
    }

    fun downloadDocument(document: Document): File? {
        val jsonResponse = getFileRequest(document.fileId)
        val response: GetFileResponse = json.decodeFromString(jsonResponse)
        response.result?.filePath?.let { filePath ->
            downloadFile(filePath, document.fileName)
        }
        val textFile = File(document.fileName)

        return if (textFile.exists()) textFile else null
    }
}
