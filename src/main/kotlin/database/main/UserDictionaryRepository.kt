package database.main

import database.DriverConnectionProviderImpl
import database.BaseJdbcRepository
import Queries
import bot.Word
import java.sql.Connection

open class UserDictionaryRepository(jdbcUrl: String, private val limit: Int) :
    BaseJdbcRepository(DriverConnectionProviderImpl(jdbcUrl)),
    UserRepository, WordRepository {

    override fun prepareTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(Queries.CREATE_TABLE_WORDS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS)
            statement.executeUpdate(Queries.CREATE_TABLE_USERS_ANSWERS)
        }
    }

    override fun addWordsToUser(chatId: Long, words: List<Word>) {
        withConnection { connection ->
            useTransaction(connection) {
                connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                    words.forEach { word ->
                        statement.setString(1, word.originalWord)
                        statement.setString(2, word.translatedWord)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
    }

    override fun addNewUser(chatId: Long, username: String) {
        withConnection { connection ->
            connection.prepareStatement(Queries.INSERT_USER).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    override fun addUserAnswersToUser(chatId: Long, words: List<Word>) {
        withConnection { connection ->
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

    override fun getUsersWordsCount(chatId: Long): Int {
        return tryOr(0) {
            withConnection { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.GET_PERSONAL_WORDS_COUNT).use { statement ->
                    statement.setLong(1, userId)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun getUsersNumOfUnlearnedWords(chatId: Long): Int {
        return tryOr(0) {
            withConnection { connection ->
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

    override fun getUsersNumOfLearnedWords(chatId: Long): Int {
        return tryOr(0) {
            withConnection { connection ->
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

    override fun getUsersLearnedWords(chatId: Long): List<Word> {
        val learned = mutableListOf<Word>()
        withConnection { connection ->
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

    override fun getUsersUnlearnedWords(chatId: Long): List<Word> {
        val unlearned = mutableListOf<Word>()
        withConnection { connection ->
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

    override fun getUsersCurrentAnswerCount(chatId: Long, word: String): Int {
        return tryOr(0) {
            withConnection { connection ->
                connection.prepareStatement(Queries.GET_CURRENT_ANSWER_COUNT).use { statement ->
                    statement.setLong(1, getUserId(connection, chatId))
                    statement.setLong(2, getWordId(connection, word))
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    override fun setUsersCorrectAnswersCount(chatId: Long, originalWord: String, correctAnswersCount: Int): Boolean {
        return tryOr(false) {
            withConnection { connection ->
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
            withConnection { connection ->
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
        withConnection { connection ->
            connection.prepareStatement(Queries.UPDATE_FILE_ID).use { statement ->
                statement.setString(1, fileId)
                statement.setString(2, originalWord)
                statement.executeUpdate()
            }
        }
    }

    override fun checkFileIdExistence(originalWord: String): Boolean {
        withConnection { connection ->
            connection.prepareStatement(Queries.CHECK_FILE_ID_EXISTENCE).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getBoolean(1) else false
            }
        }
    }

    override fun getFileId(originalWord: String): String? {
        withConnection { connection ->
            connection.prepareStatement(Queries.GET_FILE_ID).use { statement ->
                statement.setString(1, originalWord)
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getString("fileId") else null
            }
        }
    }
}
