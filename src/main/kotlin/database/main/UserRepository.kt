package database.main

import database.Queries
import dictionary.Word
import java.sql.Connection

interface UserRepository {
    fun addWords(chatId: Long, words: List<Word>)
    fun addUser(chatId: Long, username: String)
    fun addUserAnswers(chatId: Long, words: List<Word>)
    fun resetUserProgress(chatId: Long): Boolean
    fun getUsersWordsCount(chatId: Long): Int
    fun getUsersNumOfLearnedWords(chatId: Long): Int
    fun getUsersNumOfUnlearnedWords(chatId: Long): Int
    fun getUsersLearnedWords(chatId: Long): List<Word>
    fun getUsersUnlearnedWords(chatId: Long): List<Word>
    fun getUsersCurrentAnswerCount(chatId: Long, originalWord: String): Int
    fun setUsersCorrectAnswersCount(chatId: Long, originalWord: String, correctAnswersCount: Int): Boolean


    fun getUserId(connection: Connection, chatId: Long): Long {
        connection.prepareStatement(Queries.GET_USER_ID).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("User id with chatId=$chatId not found")
        }
    }
}