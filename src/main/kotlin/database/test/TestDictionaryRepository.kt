package database.test

import Queries
import database.main.UserDictionaryRepository
import bot.Word
import java.io.File
import kotlin.use

class TestDictionaryRepository(
    jdbcUrl: String,
    private val username: String,
    private val chatId: Long,
    limit: Int = 3
) : UserDictionaryRepository(jdbcUrl, limit) {

    fun insertTestData(words: File) {
        val wordsList: MutableList<Word> = mutableListOf()
        words.forEachLine { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                val originalWord = parts[0].trim()
                val translate = parts[1].trim()
                val correctAnswersCount = parts[2].trim().toIntOrNull() ?: 0
                if (originalWord.isNotBlank() && translate.isNotBlank()) {
                    wordsList.add(Word(originalWord, translate, correctAnswersCount))
                }
            }
        }
        getConnection().use { connection ->
            addUser(chatId, username)
            addWords(chatId, wordsList)
            addUserAnswers(chatId, wordsList)

        }
    }

    override fun addUserAnswers(chatId: Long, words: List<Word>) {
        getConnection().use { connection ->
            useTransaction(connection) {
                connection.prepareStatement(Queries.TEST_INSERT_USER_ANSWERS).use { statement ->
                    words.forEach { w ->
                        statement.setLong(1, 0L)
                        statement.setString(2, w.originalWord)
                        statement.setInt(3, w.correctAnswersCount)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
    }

}
