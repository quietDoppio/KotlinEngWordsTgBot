package database.main

import database.Queries
import dictionary.Word
import java.sql.Connection

interface DictionaryRepository {
    fun insertWords(words: List<Word>, chatId: Long)
    fun insertUser(username: String, chatId: Long)
    fun insertUserAnswers(words: List<Word>, chatId: Long)
    fun getWordsCount(chatId: Long): Int
    fun getNumOfLearnedWords(chatId: Long): Int
    fun getNumOfUnlearnedWords(chatId: Long): Int
    fun getLearnedWords(chatId: Long): List<Word>
    fun getUnlearnedWords(chatId: Long): List<Word>
    fun getCurrentAnswerCount(originalWord: String, chatId: Long): Int
    fun setCorrectAnswersCount(originalWord: String, correctAnswersCount: Int, chatId: Long): Boolean
    fun resetUserProgress(chatId: Long): Boolean
    fun getFileId(originalWord: String): String?
    fun updateFileId(fileId: String, originalWord: String)
    fun checkFileIdExistence(originalWord: String): Boolean

    fun getUserId(connection: Connection, chatId: Long): Long {
        connection.prepareStatement(Queries.GET_USER_ID).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("User id with chatId=$chatId not found")
        }
    }

    fun getWordId(connection: Connection, originalWord: String): Long {
        connection.prepareStatement(Queries.GET_WORD_ID).use { statement ->
            statement.setString(1, originalWord)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("Word id from word=$originalWord not found")
        }
    }
}