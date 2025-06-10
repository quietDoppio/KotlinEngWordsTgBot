package database

import java.sql.Connection
import java.sql.DriverManager

abstract class BaseJdbcRepository(private val jdbcUrl: String) : SafeExecutor {
    protected fun getConnection(): Connection {
        return DriverManager.getConnection(jdbcUrl).also { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON")
            }
        }
    }

    protected abstract fun createTables(connection: Connection)

    fun initialize() {
        getConnection().use { connection -> createTables(connection) }
    }

    fun clearData() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(Queries.DELETE_FROM_USER_ANSWERS)
                statement.executeUpdate(Queries.DELETE_FROM_USERS)
                statement.executeUpdate(Queries.DELETE_FROM_WORDS)
            }
        }
    }
}