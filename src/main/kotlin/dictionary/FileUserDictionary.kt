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
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(Queries.CREATE_TABLE_WORDS)
                statement.executeUpdate(Queries.CREATE_TABLE_USERS)
                statement.executeUpdate(Queries.CREATE_TABLE_USERS_ANSWERS)
            }
        }
    }

    fun updateFileId(fileId: String, originalWord: String) {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.UPDATE_FILE_ID).use { statement ->
                statement.setString(1, fileId)
                statement.setString(2, originalWord)
                statement.executeUpdate()
            }
        }
    }

    fun checkFileIdExistence(originalWord: String): Boolean {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.CHECK_FILE_ID_EXISTENCE).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getBoolean(1) else false
            }
        }
    }

    fun getFileId(originalWord: String): String? {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_FILE_ID).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getString(1) else null
            }
        }
    }

    fun updateDictionary(newWords: List<Word>, chatId: Long) {
        getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                    newWords.forEach { word ->
                        statement.setString(1, word.originalWord)
                        statement.setString(2, word.translatedWord)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                    connection.commit()
                }
                insertUserAnswers(chatId, newWords)
            } catch (e: Exception) {
                connection.rollback()
                println(e.message)
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun insertUserAnswers(chatId: Long, words: List<Word>) {
        getConnection().use { connection ->
            try {
                connection.autoCommit = false
                connection.prepareStatement(Queries.INSERT_USER_ANSWERS).use { statement ->
                    words.forEach { word ->
                        statement.setLong(1, chatId)
                        statement.setString(2, word.originalWord)
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
    }

    fun insertUser(chatId: Long, username: String) {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.INSERT_USER).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    fun getCurrentAnswerCount(word: String, chatId: Long): Int {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_CURRENT_ANSWER_COUNT).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, word)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    fun getSize(chatId: Long): Int {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_PERSONAL_WORDS_COUNT).use { statement ->
                statement.setLong(1, chatId)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    fun getNumOfUnlearnedWords(chatId: Long): Int {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_NUM_OF_UNLEARNED_WORDS).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    fun getNumOfLearnedWords(chatId: Long): Int {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_NUM_OF_LEARNED_WORDS).use { statement ->
                statement.setLong(1, chatId)
                statement.setInt(2, limit)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    fun getLearnedWords(chatId: Long): List<Word> {
        val learned = mutableListOf<Word>()
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_LEARNED_WORDS).use { statement ->
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
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_UNLEARNED_WORDS).use { statement ->
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
        getConnection().use { connection ->
            connection.prepareStatement(Queries.SET_CORRECT_ANSWERS_COUNT).use { statement ->
                statement.setInt(1, correctAnswersCount)
                statement.setLong(2, chatId)
                statement.setString(3, word)
                statement.executeUpdate()
            }
        }
    }

    fun resetUserProgress(chatId: Long): Boolean {
        return try {
            getConnection().use { connection ->
                connection.prepareStatement(Queries.RESET_USER_PROGRESS).use { statement ->
                    statement.setLong(1, chatId)
                    statement.executeUpdate()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun insertTestData(words: File) {
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
                connection.prepareStatement(Queries.INSERT_USER).use { statement ->
                    statement.setLong(1, 0L)
                    statement.setString(2, "testUser")
                    statement.executeUpdate()
                }
                connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                    linesTriple.forEach { t ->
                        statement.setString(1, t.first)
                        statement.setString(2, t.second)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.prepareStatement(Queries.TEST_INSERT_USER_ANSWERS).use { statement ->
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

    fun deleteData() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(Queries.DELETE_FROM_USER_ANSWERS)
                statement.executeUpdate(Queries.DELETE_FROM_USERS)
                statement.executeUpdate(Queries.DELETE_FROM_WORDS)
            }
        }
    }

}
