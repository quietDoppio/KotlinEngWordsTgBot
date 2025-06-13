package NEW

import java.sql.Connection

interface ConnectionProvider {
    fun getConnection(): Connection
}