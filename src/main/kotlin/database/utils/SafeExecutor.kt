package database.utils

import java.sql.Connection

interface SafeExecutor {
    fun <T> tryOr(default: T, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            println(e.message)
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