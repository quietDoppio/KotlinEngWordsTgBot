package dictionary

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.ResultSet

fun main() {
    val fileUserDictionary = FileUserDictionary()
    println(fileUserDictionary.getSize())
    Runtime.getRuntime().addShutdownHook(Thread {
        Database.closeConnection()
    })
}

interface IUserDictionary {
    fun updateDictionary()
    fun getNumOfLearnedWords(): Int
    fun getSize(): Int
    fun getLearnedWords(): List<Word>
    fun getUnlearnedWords(): List<Word>
    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int)
    fun resetUserProgress()
}

object Database {
    private val config = HikariConfig().apply {
        jdbcUrl = "jdbc:sqlite:sample.bd"
        maximumPoolSize = 1

    }
    private val dataSource = HikariDataSource(config)
    fun getConnection() = dataSource.connection
    fun closeConnection() = dataSource.close()
}

class FileUserDictionary() : IUserDictionary {

    init {
        createTables()
        updateDictionary()
    }

    fun createTables() {
        val queryWordsTable = """
        CREATE TABLE IF NOT EXISTS words (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        text VARCHAR UNIQUE,
        translate VARCHAR
        )
    """.trimIndent()
        val queryCreateUsers = """
        CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        chat_id INTEGER UNIQUE,
        user_name VARCHAR
        )
        """.trimIndent()
        val queryCreateUserAnswers = """
        CREATE TABLE IF NOT EXISTS user_answers (
        user_id INTEGER,
        word_id INTEGER,
        correct_answer_count INTEGER DEFAULT 0,
        PRIMARY KEY (user_id, word_id),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
        FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE
        )
    """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(queryWordsTable)
                statement.executeUpdate(queryCreateUsers)
                statement.executeUpdate(queryCreateUserAnswers)
            }
        }
    }

    fun insertUserAnswers(chatId: Long) {
        val querySelectUsersAndWords = """
            SELECT u.id AS user_id, w.id AS word_id 
            FROM users u 
            CROSS JOIN words w
            ORDER BY u.id, w.id
        """.trimMargin()

        val queryInsert = """
            INSERT OR IGNORE INTO user_answers (user_id, word_id)
            VALUES (?, ?)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val idsResultSet: ResultSet
                connection.createStatement().use { statement ->
                    idsResultSet = statement.executeQuery(querySelectUsersAndWords)

                    connection.prepareStatement(queryInsert).use { statement ->
                        while (idsResultSet.next()) {
                            statement.setInt(1, idsResultSet.getInt("user_id"))
                            statement.setInt(2, idsResultSet.getInt("word_id"))
                            statement.addBatch()
                        }
                        statement.executeBatch()

                    }
                    connection.commit()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                connection.rollback()
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun insertUser(chatId: Long, username: String) {
        val queryInsert = "INSERT OR IGNORE INTO users (chat_id, user_name) VALUES (?, ?)"
        Database.getConnection().use { connection ->
            connection.prepareStatement(queryInsert).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    override fun updateDictionary() {
        val words = File("words.txt")
        val queryInsertWords = """
            INSERT OR IGNORE INTO words (text, translate)
            VALUES (?, ?)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(queryInsertWords).use { statement ->
                    words.forEachLine { line ->
                        val parts = line.split("|")
                        statement.setString(1, parts[0])
                        statement.setString(2, parts[1])
                        statement.addBatch()
                    }
                    statement.executeBatch()
                    connection.commit()
                }
            } catch (e: Exception) {
                println(e)
                connection.rollback()
            } finally {
                connection.autoCommit = true
            }
        }
    }

    override fun getSize(): Int {
        var count = 0
        val queryGetCount = "SELECT COUNT(*) FROM words"
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(queryGetCount)
                if (resultSet.next()) {
                    count = resultSet.getInt(1)
                }
            }
        }
        return count
    }

    override fun getNumOfLearnedWords(): Int {
        //TODO("Not yet implemented")
        return 0
    }

    override fun getLearnedWords(): List<Word> {
        //TODO("Not yet implemented")
        return emptyList()
    }

    override fun getUnlearnedWords(): List<Word> {
        //TODO("Not yet implemented")
        return emptyList()
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        //TODO("Not yet implemented")
    }

    override fun resetUserProgress() {
        //TODO("Not yet implemented")
    }

}
