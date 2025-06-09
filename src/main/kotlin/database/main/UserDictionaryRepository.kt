package database.main

import database.BaseJdbcRepository
import database.Queries
import dictionary.Word
import java.io.File
import java.sql.Connection

class UserDictionaryRepository(jdbcUrl: String, private val limit: Int) : BaseJdbcRepository(jdbcUrl),
    DictionaryRepository {

    override fun createTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(Queries.CREATE_TABLE_WORDS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS_ANSWERS)
        }
    }

    override fun insertWords(newWords: List<Word>, chatId: Long) {
        getConnection().use { connection ->
            useTransaction(connection) {
                connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                    newWords.forEach { word ->
                        statement.setString(1, word.originalWord)
                        statement.setString(2, word.translatedWord)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
    }

    override fun insertUser(username: String, chatId: Long) {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.INSERT_USER).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    override fun insertUserAnswers(words: List<Word>, chatId: Long) {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.INSERT_USER_ANSWERS).use { statement ->
                val userId = getUserId(connection, chatId)
                words.forEach { word ->
                    val wordId = getWordId(connection, word.originalWord)
                    statement.setLong(1, userId)
                    statement.setLong(2, wordId)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    override fun getWordsCount(chatId: Long): Int {
        return tryOr(0) {
            getConnection().use { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.GET_PERSONAL_WORDS_COUNT).use { statement ->
                    statement.setLong(1, userId)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun getNumOfUnlearnedWords(chatId: Long): Int {
        return tryOr(0) {
            getConnection().use { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.GET_NUM_OF_UNLEARNED_WORDS).use { statement ->
                    statement.setLong(1, userId)
                    statement.setInt(2, limit)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun getNumOfLearnedWords(chatId: Long): Int {
        return tryOr(0) {
            getConnection().use { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.GET_NUM_OF_LEARNED_WORDS).use { statement ->
                    statement.setLong(1, userId)
                    statement.setInt(2, limit)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun getLearnedWords(chatId: Long): List<Word> {
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

    override fun getUnlearnedWords(chatId: Long): List<Word> {
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

    override fun getCurrentAnswerCount(word: String, chatId: Long): Int {
        return tryOr(0) {
            getConnection().use { connection ->
                connection.prepareStatement(Queries.GET_CURRENT_ANSWER_COUNT).use { statement ->
                    statement.setLong(1, getUserId(connection, chatId))
                    statement.setLong(2, getWordId(connection, word))
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun setCorrectAnswersCount(originalWord: String, correctAnswersCount: Int, chatId: Long): Boolean {
        return tryOr(false) {
            getConnection().use { connection ->
                val userId = getUserId(connection, chatId)
                val wordId = getWordId(connection, originalWord)
                connection.prepareStatement(Queries.SET_CORRECT_ANSWERS_COUNT).use { statement ->
                    statement.setInt(1, correctAnswersCount)
                    statement.setLong(2, userId)
                    statement.setLong(3, wordId)
                    statement.executeUpdate()
                    true
                }
            }
        }
    }

    override fun resetUserProgress(chatId: Long): Boolean {
        return tryOr(false) {
            getConnection().use { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.RESET_USER_PROGRESS).use { statement ->
                    statement.setLong(1, userId)
                    statement.executeUpdate()
                    true
                }
            }
        }
    }

    override fun updateFileId(fileId: String, originalWord: String) {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.UPDATE_FILE_ID).use { statement ->
                statement.setString(1, fileId)
                statement.setString(2, originalWord)
                statement.executeUpdate()
            }
        }
    }

    override fun checkFileIdExistence(originalWord: String): Boolean {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.CHECK_FILE_ID_EXISTENCE).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getBoolean(1) else false
            }
        }
    }

    override fun getFileId(originalWord: String): String? {
        getConnection().use { connection ->
            connection.prepareStatement(Queries.GET_FILE_ID).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getString("fileId") else null
            }
        }
    }

    override fun getUserId(connection: Connection, chatId: Long): Long {
        connection.prepareStatement(Queries.GET_USER_ID).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("User id with chatId=$chatId not found")
        }
    }

    override fun getWordId(connection: Connection, originalWord: String): Long {
        connection.prepareStatement(Queries.GET_WORD_ID).use { statement ->
            statement.setString(1, originalWord)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("Word id from word=$originalWord not found")
        }
    }

}
