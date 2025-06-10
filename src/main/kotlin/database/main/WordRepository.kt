package database.main

import database.Queries
import java.sql.Connection

interface WordRepository {
    fun getFileId(originalWord: String): String?
    fun updateFileId(fileId: String, originalWord: String)
    fun checkFileIdExistence(originalWord: String): Boolean

    fun getWordId(connection: Connection, originalWord: String): Long {
        connection.prepareStatement(Queries.GET_WORD_ID).use { statement ->
            statement.setString(1, originalWord)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong(1)
            else throw IllegalStateException("Word id from word=$originalWord not found")
        }
    }
}