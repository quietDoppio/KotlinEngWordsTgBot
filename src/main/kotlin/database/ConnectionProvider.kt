package database

import java.sql.Connection
import java.sql.DriverManager

class ConnectionProvider(private val jdbcUrl: String) {
    private fun getConnection(): Connection = DriverManager.getConnection(jdbcUrl)
        .also { conn ->
            conn.createStatement().use { stmt -> stmt.execute("PRAGMA foreign_keys = ON") }
        }

    fun <T> withConnection(block: (Connection) -> T): T = getConnection().use(block)

    fun <T> tryOr(default: T, message: String = "", block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            println(message)
            e.printStackTrace()
            default
        }
    }

   fun useTransaction(connection: Connection, block: () -> Unit) {
        connection.autoCommit = false
        try {
            block()
            connection.commit()
        } catch (e: Exception) {
            e.printStackTrace()
            connection.rollback()
        } finally {
            connection.autoCommit = true
        }
    }
}