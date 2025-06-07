package database.main

import database.ConnectionProvider
import database.Queries
import java.sql.Connection
import kotlin.use

class WordRepository(private val provider: ConnectionProvider) {
    fun updateFileId(chatId: Long, fileId: String, originalWord: String) {
        provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.UPDATE_FILE_ID).use { statement ->
                statement.setString(1, fileId)
                statement.setLong(2, userId)
                statement.setString(3, originalWord)
                statement.executeUpdate()
            }
        }
    }

    fun checkFileIdExistence(chatId:Long, originalWord: String): Boolean {
        return provider.withConnection { connection ->
            val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.CHECK_FILE_ID_EXISTENCE).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, originalWord)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) resultSet.getBoolean(1) else false
            }
        }
    }

   fun getFileId(chatId: Long, originalWord: String): String? {
       return provider.withConnection { connection ->
           val userId = getUserId(connection, chatId)
            connection.prepareStatement(Queries.GET_FILE_ID).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, originalWord)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) resultSet.getString("fileId") else null
            }
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
