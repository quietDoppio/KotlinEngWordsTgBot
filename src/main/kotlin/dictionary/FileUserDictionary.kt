package dictionary

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

fun main() {
    var dataSource = FileUserDictionary()
    dataSource.getSize()
    Runtime.getRuntime().addShutdownHook(Thread{
        FileUserDictionary.Database.close()
    })
}

class FileUserDictionary(): IUserDictionary {
    object Database {
        private val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:data.db"
            maximumPoolSize = 1
        }
        private val dataSource = HikariDataSource(hikariConfig)
        fun getConnection(): Connection = dataSource.connection
        fun close() {
            dataSource.close()
        }
    }
    init {
        createTable()
        updateDictionary(File("words.txt"))
    }
    private fun createTable() {
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                try {
                    statement.executeUpdate(
                        """
                    CREATE TABLE IF NOT EXISTS 'words'(
                    'id' integer PRIMARY KEY AUTOINCREMENT,
                    'text' varchar UNIQUE NOT NULL,
                    'translate' varchar NOT NULL
                    );
                        """.trimIndent()
                    )
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                }
            }
        }
    }

    fun updateDictionary(wordsFile: File) {
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM words")
                statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'words'")
            }
            try {
                connection.autoCommit = false
                connection.prepareStatement("INSERT OR IGNORE INTO words ('text', 'translate') VALUES (?, ?)")
                    .use { statement ->
                        wordsFile.forEachLine { line ->
                            if (line.trim().isNotBlank() && line.contains("|")) {
                                val parts = line.split("|")
                                statement.setString(1, parts[0])
                                statement.setString(2, parts[1])
                                statement.addBatch()
                            }
                        }

                        statement.executeBatch()
                        connection.commit()
                    }
            } catch (e: SQLException) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }

    }

    override fun getNumOfLearnedWords(): Int {
        TODO("Not yet implemented")
    }

    override fun getSize(): Int {
        var count = 0
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
               val resultSet = statement.executeQuery("SELECT COUNT(*) FROM words")
                if(resultSet.next()) {
                    count = resultSet.getInt(1)
                }
            }
        }
        return count
    }

    override fun getLearnedWords(): List<Word> {
        TODO("Not yet implemented")
    }

    override fun getUnlearnedWords(): List<Word> {
        TODO("Not yet implemented")
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        TODO("Not yet implemented")
    }

    override fun resetUserProgress() {
        TODO("Not yet implemented")
    }

}
interface IUserDictionary {
    fun getNumOfLearnedWords(): Int
    fun getSize(): Int
    fun getLearnedWords(): List<Word>
    fun getUnlearnedWords(): List<Word>
    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int)
    fun resetUserProgress()
}




