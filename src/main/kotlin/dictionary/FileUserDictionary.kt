package dictionary

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class FileUserDictionary(val limit: Int, val jdbcUrl: String) {

    private fun getConnection(): Connection {
        return DriverManager.getConnection(jdbcUrl).also { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON")
            }
        }
    }

    init {
        createTables()
    }

    private fun createTables() {
        val queryWordsTable = """
        CREATE TABLE IF NOT EXISTS words (
        id INTEGER PRIMARY KEY,
        user_id INTEGER,
        text VARCHAR,
        translate VARCHAR,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
        UNIQUE (text, user_id)
        )
    """.trimIndent()
        val queryCreateUsers = """
        CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
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
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(queryWordsTable)
                statement.executeUpdate(queryCreateUsers)
                statement.executeUpdate(queryCreateUserAnswers)
            }
        }
    }

    fun updateDictionary(newWords: List<Word>, chatId: Long) {
        val queryUpdateDictionary ="""
                INSERT OR IGNORE INTO words (text, translate, user_id) 
                VALUES (
                    ?, 
                    ?, 
                    (SELECT id FROM users WHERE chat_id = ?)
                ) 
        """.trimMargin().trim()
        getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(queryUpdateDictionary).use { statement ->
                    newWords.forEach { word ->
                            val text = word.originalWord
                            val translate = word.translatedWord
                            statement.setString(1, text)
                            statement.setString(2, translate)
                            statement.setLong(3, chatId)
                            statement.addBatch()
                        }
                    statement.executeBatch()
                    connection.commit()
                }
            } catch (e: Exception) {
                connection.rollback()
                println(e.message)
            } finally {
                connection.autoCommit = true
            }
        }
        insertUserAnswers(chatId)
    }

    fun deleteData() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM user_answers")
                statement.executeUpdate("DELETE FROM users")
                statement.executeUpdate("DELETE FROM words")
            }
        }
    }

    fun insertTestData(words: File) {
        val queryInsertUser = "INSERT OR REPLACE INTO users (chat_id, user_name) VALUES (?, ?)"
        val queryInsertWords = """
            INSERT OR REPLACE INTO words (user_id, text, translate)
             VALUES (
                (SELECT id FROM users WHERE chat_id = ?), ?, ?
             )
            
        """.trimIndent()
        val queryInsertUserAnswers = """
            INSERT OR REPLACE INTO user_answers (user_id, word_id, correct_answer_count)
            VALUES (
            (SELECT id FROM users WHERE chat_id = ?), 
            (SELECT id FROM words WHERE text = ?),
             ?
           )
        """.trimIndent()
        val linesTriple = words.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                val word = parts[0].trim()
                val translate = parts[1].trim()
                val correctAnswersCount = parts[2].trim().toIntOrNull() ?: 0
                if (word.isNotEmpty() && translate.isNotEmpty()) {
                    Triple<String, String, Int>(word, translate, correctAnswersCount)
                } else null
            } else null
        }

        getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(queryInsertUser).use { statement ->
                    statement.setLong(1, 0L)
                    statement.setString(2, "testUser")
                    statement.executeUpdate()
                }
                connection.prepareStatement(queryInsertWords).use { statement ->
                    linesTriple.forEach { t ->
                        statement.setLong(1, 0L)
                        statement.setString(2, t.first)
                        statement.setString(3, t.second)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.prepareStatement(queryInsertUserAnswers).use { statement ->
                    linesTriple.forEach { t ->
                        statement.setLong(1, 0L)
                        statement.setString(2, t.first)
                        statement.setInt(3, t.third)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun insertUser(chatId: Long, username: String) {
        val queryInsert = "INSERT OR IGNORE INTO users (chat_id, user_name) VALUES (?, ?)"
        getConnection().use { connection ->
            connection.prepareStatement(queryInsert).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    private fun insertUserAnswers(chatId: Long) {
        val queryInsert = """
            INSERT OR IGNORE INTO user_answers (user_id, word_id)
            SELECT u.id, w.id
            FROM words w
            JOIN users u ON u.id = w.user_id
            WHERE u.chat_id = ?
            ORDER BY u.id, w.id
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(queryInsert).use { statement ->
                statement.setLong(1, chatId)
                statement.executeUpdate()
            }
        }
    }

    fun getCurrentAnswerCount(word: String, chatId: Long): Int {
        val queryAnswerCount = """
            SELECT correct_answer_count AS count
            FROM user_answers ua
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
            AND word_id = (
                SELECT id FROM words
                WHERE text = ?
                AND user_id = (SELECT id FROM users WHERE chat_id = ?)
            )
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(queryAnswerCount).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, word)
                statement.setLong(3, chatId)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt("count") else 0
            }
        }
    }

    fun getSize(chatId: Long): Int {
        val queryGetCount = "SELECT COUNT(*) AS count FROM words WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)"
        getConnection().use { connection ->
            connection.prepareStatement(queryGetCount).use { statement ->
                statement.setLong(1, chatId)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt("count") else 0
            }
        }
    }

    fun getNumOfUnlearnedWords(chatId: Long): Int {
        val querySelectCountOfUnlearned = """
            SELECT COUNT(*) AS count
            FROM user_answers ua
            JOIN users u ON u.id = ua.user_id
            JOIN words w ON w.id = ua.word_id
            WHERE u.chat_id = ? AND ua.correct_answer_count < ?
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(querySelectCountOfUnlearned).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt("count") else 0
            }
        }
    }

    fun getNumOfLearnedWords(chatId: Long): Int {
        val querySelectCountOfLearned = """
            SELECT COUNT(*) AS count
            FROM user_answers ua
            JOIN users u ON ua.user_id = u.id         
            WHERE u.chat_id = ? AND ua.correct_answer_count >= ?                 
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(querySelectCountOfLearned).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt("count") else 0
            }
        }
    }

    fun getLearnedWords(chatId: Long): List<Word> {
        val learned = mutableListOf<Word>()
        val querySelectLearned = """
            SELECT w.text AS text, w.translate AS translate
            FROM user_answers ua
            JOIN users u ON ua.user_id = u.id
            JOIN words w ON ua.word_id = w.id
            WHERE u.chat_id = ? AND ua.correct_answer_count >= ?                 
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(querySelectLearned).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val text = resultSet.getString("text")
                    val translate = resultSet.getString("translate")
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
            WHERE u.chat_id = ? AND ua.correct_answer_count < ?
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(querySelectUnlearned).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
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
            AND word_id = (
                SELECT id FROM words
                WHERE text = ?
                AND user_id = (SELECT id FROM users WHERE chat_id = ?)        
            )
        """.trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(querySetCount).use { statement ->
                statement.setInt(1, correctAnswersCount)
                statement.setLong(2, chatId)
                statement.setString(3, word)
                statement.setLong(4, chatId)
                statement.executeUpdate()
            }
        }
    }

    fun resetUserProgress(chatId: Long): Boolean {
        val queryReset = """
            UPDATE user_answers 
            SET correct_answer_count = 0
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
        """.trimIndent()
        return try {
            getConnection().use { connection ->
                connection.prepareStatement(queryReset).use { statement ->
                    statement.setLong(1, chatId)
                    statement.executeUpdate()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

}
