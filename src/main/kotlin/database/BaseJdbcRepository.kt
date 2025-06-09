package database

import SafeExecutor
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
}