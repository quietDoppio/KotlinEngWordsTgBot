package database.test

import database.BaseJdbcRepository
import database.Queries
import java.io.File
import java.sql.Connection
import kotlin.use

class TestDictionaryRepository(jdbcUrl: String, private val username: String, private val chatId: Long) :
    BaseJdbcRepository(jdbcUrl),
    TestRepository {
    override fun createTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(Queries.CREATE_TABLE_WORDS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS_ANSWERS)
        }
    }

    fun insertTestData(words: File) {
        val wordsList: MutableList<TestWord> = mutableListOf()
        words.forEachLine { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                val originalWord = parts[0].trim()
                val translate = parts[1].trim()
                val correctAnswersCount = parts[2].trim().toIntOrNull() ?: 0
                if (originalWord.isNotBlank() && translate.isNotBlank()) {
                    wordsList.add(TestWord(originalWord, translate, correctAnswersCount))
                }
            }
        }
        getConnection().use { connection ->
            useTransaction(connection) {
                insertUser(connection, username, chatId)
                insertWords(connection, wordsList, chatId)
                insertUserAnswers(connection, wordsList, chatId)
            }
        }
    }

    override fun insertWords(connection: Connection, words: List<TestWord>, chatId: Long) {
        connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
            words.forEach { w ->
                statement.setString(1, w.originalWord)
                statement.setString(2, w.translate)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    override fun insertUser(connection: Connection, username: String, chatId: Long) {
        connection.prepareStatement(Queries.INSERT_USER).use { statement ->
            statement.setLong(1, chatId)
            statement.setString(2, username)
            statement.executeUpdate()
        }
    }

    override fun insertUserAnswers(connection: Connection, words: List<TestWord>, chatId: Long) {
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

    override fun deleteData() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(Queries.DELETE_FROM_USER_ANSWERS)
                statement.executeUpdate(Queries.DELETE_FROM_USERS)
                statement.executeUpdate(Queries.DELETE_FROM_WORDS)
            }
        }
    }
}

data class TestWord(
    val originalWord: String,
    val translate: String,
    val correctAnswersCount: Int
)