package database

import java.sql.Connection
import java.sql.DriverManager
import kotlin.use

class ConnectionProvider(private val jdbcUrl: String) {
   fun getConnection(): Connection = DriverManager.getConnection(jdbcUrl)
        .also { conn ->
            conn.createStatement().use { stmt -> stmt.execute("PRAGMA foreign_keys = ON") }
        }

    fun <T> withConnection(block: (Connection) -> T): T =
        getConnection().use(block)

    fun <T> tryOr(default: T, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
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
            println(e)
            connection.rollback()
        } finally {
            connection.autoCommit = true
        }
    }
}
