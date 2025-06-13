package database

import NEW.ConnectionProvider
import NEW.DataCleaner
import NEW.TableInitializer
import java.sql.Connection

abstract class BaseJdbcRepository(protected val connectionProvider: ConnectionProvider) :
    TableInitializer, DataCleaner, SafeExecutor {
    protected inline fun <T> withConnection(block: (Connection) -> T): T =
        connectionProvider.getConnection().use(block)

    protected abstract fun prepareTables(connection: Connection)

    override fun initTables() {
        withConnection { connection -> prepareTables(connection) }
    }

    override fun clearData(tableNames: List<String>) {
        withConnection { connection ->
            connection.createStatement().use { statement ->
                tableNames.forEach { name ->
                    require(name.matches(Regex("[A-Za-z0-9_]+")))
                    statement.executeUpdate("${Queries.DELETE_FROM} $name")
                }
            }
        }
    }
}