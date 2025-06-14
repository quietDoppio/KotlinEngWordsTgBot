package database

import java.sql.Connection
import java.sql.DriverManager
import kotlin.use

interface ConnectionProvider {
    fun getConnection(): Connection
}

class DriverConnectionProviderImpl(private val jdbcUrl: String) : ConnectionProvider {
    override fun getConnection(): Connection = DriverManager.getConnection(jdbcUrl)
        .also { conn ->
            conn.createStatement().use { stmt -> stmt.execute("PRAGMA foreign_keys = ON") }
        }
}