package dictionary

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object Database {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:sqlite:data.db"
        maximumPoolSize = 10
    }
    private val dataSource = HikariDataSource(hikariConfig)
    fun getConnection(): Connection = dataSource.connection
    fun close() {
        dataSource.close()
    }
}