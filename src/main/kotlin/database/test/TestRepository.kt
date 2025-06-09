package database.test

import java.sql.Connection

interface TestRepository {
    fun insertWords(connection: Connection, words: List<TestWord>, chatId: Long)
    fun insertUser(connection: Connection ,username: String, chatId: Long)
    fun insertUserAnswers(connection: Connection, words: List<TestWord>, chatId: Long)
    fun deleteData()
}