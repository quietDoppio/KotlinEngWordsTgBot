package dictionary

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File

class FileUserDictionary() {
    private object Database {
        private val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:sample.bd"
            maximumPoolSize = 1
        }
        private val dataSource = HikariDataSource(config)
        fun getConnection() = dataSource.connection
        fun closeConnection() {
            if (!dataSource.isClosed) {
                dataSource.close()
            }
        }
        init {
            Runtime.getRuntime().addShutdownHook(Thread{
                closeConnection()
            })
        }
    }

    init {
        createTables()
        updateDictionary()
    }

    private fun createTables() {
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

    private fun updateDictionary() {
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

    fun insertUser(chatId: Long, username: String) {
        val queryInsert = "INSERT OR IGNORE INTO users (chat_id, user_name) VALUES (?, ?)"
        Database.getConnection().use { connection ->
            connection.prepareStatement(queryInsert).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
        insertUserAnswers()
    }

    private fun insertUserAnswers() {
        val queryInsert = """
            INSERT OR IGNORE INTO user_answers (user_id, word_id)
            SELECT u.id, w.id
            FROM users u
            CROSS JOIN words w
            ORDER BY u.id, w.id
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(queryInsert).use { statement ->
                statement.executeBatch()
            }
        }
    }

    fun getSize(): Int {
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

    fun getNumOfLearnedWords(chatId: Long): Int {
        var count = 0
        val querySelectCountOfLearned = """
            SELECT COUNT(*) AS count
            FROM user_answers ua
            JOIN users u ON ua.user_id = u.id         
            WHERE u.chat_id = ? AND ua.correct_answer_count >= 3                 
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(querySelectCountOfLearned).use { statement ->
                statement.setLong(1, chatId)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    count = resultSet.getInt("count")
                }
            }
        }
        return count
    }

    fun getLearnedWords(chatId: Long): List<Word> {
        val learned = mutableListOf<Word>()
        val querySelectLearned = """
            SELECT w.text, w.translate
            FROM user_answers ua
            JOIN users u ON ua.user_id = u.id
            JOIN words w ON ua.word_id = w.id
            WHERE u.chat_id = ? AND ua.correct_answer_count >= 3                 
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(querySelectLearned).use { statement ->
                statement.setLong(1, chatId)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val text = resultSet.getString(1)
                    val translate = resultSet.getString(2)
                    learned.add(Word(text, translate))
                }
            }
        }
        return learned
    }

    fun getUnlearnedWords(chatId: Long): List<Word> {
        val unlearned = mutableListOf<Word>()
        val querySelectUnlearned = """
            SELECT w.text AS text, w.translate AS translate
            FROM user_answers ua
            JOIN words w ON ua.word_id = w.id
            JOIN users u ON ua.user_id = u.id
            WHERE u.chat_id = ? AND ua.correct_answer_count < 3
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(querySelectUnlearned).use { statement ->
                statement.setLong(1, chatId)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val text = resultSet.getString("text")
                    val translate = resultSet.getString("translate")
                    unlearned.add(Word(text, translate))
                }
            }
        }
        return unlearned
    }

    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int, chatId: Long) {
        val querySetCount = """
            UPDATE user_answers 
            SET correct_answer_count = ?
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
                  AND word_id = (SELECT id FROM words WHERE text = ?)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(querySetCount).use { statement ->
                statement.setInt(1, correctAnswersCount)
                statement.setLong(2, chatId)
                statement.setString(3, word)
                statement.executeUpdate()
            }
        }
    }

    fun resetUserProgress(chatId: Long) {
        val queryReset = """
            UPDATE user_answers 
            SET correct_answer_count = 0
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.prepareStatement(queryReset).use { statement ->
                statement.setLong(1, chatId)
                statement.executeUpdate()
            }
        }
    }

}
