package database

import kotlin.use

class TablesHandler(private val provider: ConnectionProvider) {
    fun initTables() {
        provider.withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(Queries.CREATE_TABLE_WORDS)
                statement.executeUpdate(Queries.CREATE_TABLE_USERS)
                statement.executeUpdate(Queries.CREATE_TABLE_USERS_ANSWERS)
            }
        }
    }
}
