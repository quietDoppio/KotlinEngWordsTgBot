package database.main

import bot.Word
import database.ConnectionProvider
import database.Queries
import java.sql.Connection

class UserRepository(private val provider: ConnectionProvider, private val limit: Int) {
    fun getUsersWordsCount(chatId: Long): Int {
        return provider.tryOr(0) {
            provider.withConnection { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.GET_PERSONAL_WORDS_COUNT).use { statement ->
                    statement.setLong(1, userId)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    fun editWord(chatId: Long, oldWord: Word, newWord: Word) {
        provider.withConnection { conn ->
            val userId = getUserId(conn, chatId)
            conn.prepareStatement(Queries.UPDATE_WORD).use { statement ->
                statement.setString(1, newWord.originalWord)
                statement.setString(2, newWord.translatedWord)
                statement.setLong(3, userId)
                statement.setString(4, oldWord.originalWord)
                statement.executeUpdate()
            }
        }
    }

    fun clearUsersWords(chatId: Long) {
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.DELETE_WORDS).use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate()
            }
        }
    }

    fun deleteWord(chatId: Long, word: String) {
        provider.withConnection { conn ->
            val userId = getUserId(conn, chatId)
            conn.prepareStatement(Queries.DELETE_WORD).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, word)
                statement.executeUpdate()
            }
        }
    }

    fun getUnlearnedWords(chatId: Long): List<Word> {
        val unlearned = mutableListOf<Word>()
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.GET_UNLEARNED_WORDS).use { statement ->
                statement.setLong(1, userId)
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

    fun getLearnedWords(chatId: Long): List<Word> {
        val learned = mutableListOf<Word>()
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.GET_LEARNED_WORDS).use { statement ->
                statement.setLong(1, userId)
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

    fun addWords(chatId: Long, words: List<Word>) {
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            provider.useTransaction(connection) {
                connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                    words.forEach { word ->
                        statement.setLong(1, userId)
                        statement.setString(2, word.originalWord)
                        statement.setString(3, word.translatedWord)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
    }

    fun addWords(chatId: Long, word: Word) {
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.INSERT_WORD).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, word.originalWord)
                statement.setString(3, word.translatedWord)
                statement.executeUpdate()
            }
        }
    }

    fun addUserAnswersToUser(chatId: Long, words: List<Word>) {
        provider.withConnection { connection ->
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

    fun addNewUser(chatId: Long, username: String) {
        provider.withConnection { connection ->
            connection.prepareStatement(Queries.INSERT_USER).use { statement ->
                statement.setLong(1, chatId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }

    fun resetUserProgress(chatId: Long): Boolean {
        return provider.tryOr(false) {
            provider.withConnection { connection ->
                val userId = getUserId(connection, chatId)
                connection.prepareStatement(Queries.RESET_USER_PROGRESS).use { statement ->
                    statement.setLong(1, userId)
                    statement.executeUpdate()
                    true
                }
            }
        }
    }

    fun getUsersNumOfLearnedWords(chatId: Long): Int {
        return provider.tryOr(0) {
            provider.withConnection { connection ->
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


    fun getUsersNumOfUnlearnedWords(chatId: Long): Int {
        return provider.tryOr(0) {
            provider.withConnection { connection ->
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

    fun getUsersCurrentAnswerCount(chatId: Long, word: String): Int {
        return provider.tryOr(0) {
            provider.withConnection { connection ->
                connection.prepareStatement(Queries.GET_CURRENT_ANSWER_COUNT).use { statement ->
                    statement.setLong(1, getUserId(connection, chatId))
                    statement.setLong(2, getWordId(connection, word))
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) resultSet.getInt("count") else 0
                }
            }
        }
    }

    fun setUsersCorrectAnswersCount(chatId: Long, originalWord: String, correctAnswersCount: Int): Boolean {
        return provider.tryOr(false) {
            provider.withConnection { connection ->
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

    fun getDictionary(chatId: Long): List<Word> {
        return provider.withConnection { conn ->
            val userId = getUserId(conn, chatId)
            val dictionary = mutableListOf<Word>()
            conn.prepareStatement(Queries.GET_DICTIONARY).use { statement ->
                statement.setLong(1, userId)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    dictionary.add(
                        Word(
                            resultSet.getString("text"),
                            resultSet.getString("translate")
                        )
                    )
                }
            }
            dictionary
        }
    }

    private fun getWordId(connection: Connection, originalWord: String): Long {
        connection.prepareStatement(Queries.GET_WORD_ID).use { statement ->
            statement.setString(1, originalWord)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("Word id from word=$originalWord not found")
        }
    }

    fun getUserId(connection: Connection, chatId: Long): Long {
        connection.prepareStatement(Queries.GET_USER_ID).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("User id with chatId=$chatId not found")
        }
    }
}