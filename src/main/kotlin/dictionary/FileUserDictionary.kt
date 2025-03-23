package dictionary

import java.io.File
import java.sql.SQLException

class FileUserDictionary() : IUserDictionary {
    private var _chatId = 0L
    val chatId get() = _chatId
    fun setChatId(chatId: Long){ this._chatId = chatId}

    init {
        createTable()
        updateDictionary(File("words.txt"))
    }
    
    private fun createTable() {
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                        CREATE TABLE IF NOT EXISTS 'words'
                        (
                        'id' integer PRIMARY KEY AUTOINCREMENT,
                        'text' varchar UNIQUE NOT NULL,
                        'translate' varchar NOT NULL
                        );
                        """.trimIndent()
                )
                statement.executeUpdate(
                    """
                        CREATE TABLE IF NOT EXISTS 'users'
                        (
                        'id' integer PRIMARY KEY AUTOINCREMENT,
                        'username' varchar NOT NULL,
                        'created_at' timestamp DEFAULT CURRENT_TIMESTAMP,
                        'chat_id' integer UNIQUE NOT NULL
                        );
                        """.trimIndent()
                )
                statement.executeUpdate(
                    """
                        CREATE TABLE IF NOT EXISTS 'user_answers'
                        (
                        'user_id' integer,
                        'word_id' integer,
                        'correct_answer_count' integer DEFAULT 0,
                        'updated_at' timestamp DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY ('user_id') REFERENCES 'users' ('id'),
                        FOREIGN KEY ('word_id') REFERENCES 'words' ('id')
                        );
                        """.trimIndent()
                )
            }
        }
    }
    fun createUser(userName: String, chatId: Long){
        val query = """
            INSERT OR IGNORE INTO users ('username', 'chat_id') VALUES (?, ?) 
        """.trimIndent()
        Database.getConnection().use { connection -> 
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, userName)
                statement.setLong(2, chatId)
                statement.executeUpdate()
            }
        }
    }
    
    private fun updateDictionary(wordsFile: File) {
        Database.getConnection().use { connection ->
            try {
                connection.autoCommit = false

                connection.createStatement().use { statement ->
                    statement.executeUpdate("DELETE FROM words")
                    statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'words'")
                }
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
        var count = 0
        val query = """
            SELECT COUNT(*) AS count
            FROM user_answers ua    
            JOIN users u ON u.id = ua.user_id 
            WHERE u.chat_id = $_chatId AND ua.correct_answer_count >= 3
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet =
                    statement.executeQuery(query)
                if (resultSet.next()) {
                    count = resultSet.getInt("count")
                }
            }
        }
        return count
    }

    override fun getSize(): Int {
        var count = 0
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT COUNT(*) FROM words")
                if (resultSet.next()) {
                    count = resultSet.getInt(1)
                }
            }
        }
        return count
    }

    override fun getLearnedWords(): List<Word> {
        val learnedWords: MutableList<Word> = mutableListOf()
        val query =  """
                    SELECT w.text AS originalWord, w.translate AS translatedWord, ua.correct_answer_count
                    FROM words w
                    JOIN user_answers ua ON ua.word_id = w.id
                    JOIN users u ON u.id = ua.user_id
                    WHERE u.chat_id = $_chatId AND ua.correct_answer_count >= 3
                    """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(query)
                while (resultSet.next()){
                    learnedWords.add(
                        Word(
                            originalWord = resultSet.getString("originalWord"),
                            translatedWord = resultSet.getString("translatedWord"),
                            correctAnswerCount = resultSet.getInt("correct_answer_count")
                        )
                    )
                }
            }
        }
        return learnedWords
    }

    override fun getUnlearnedWords(): List<Word> {
        val unlearnedWords = mutableListOf<Word>()
        val query = """
            SELECT w.text AS originalWord, w.translate AS translatedWord, ua.correct_answer_count
            FROM words w
            JOIN user_answers ua ON ua.word_id = w.id
            JOIN users u ON u.id = ua.user_id
            WHERE u.chat_id = $_chatId AND ua.correct_answer_count < 3
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(query)
                while (resultSet.next()){
                    unlearnedWords.add(
                        Word(
                            originalWord = resultSet.getString("originalWord"),
                            translatedWord = resultSet.getString("translatedWord"),
                            correctAnswerCount = resultSet.getInt("correct_answer_count")
                        )
                    )
                }
            }
        }
        return unlearnedWords
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val update = """
            UPDATE user_answers
            SET correct_answer_count = $correctAnswersCount 
            WHERE word_id = (SELECT id FROM words WHERE text = $word)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(update)
            }
        }
    }

    override fun resetUserProgress() {
        val update = """
            UPDATE user_answers
            SET correct_answer_count = 0
            WHERE user_id = (SELECT id FROM users WHERE chat_id = $_chatId)
        """.trimIndent()
        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(update)
            }
        }
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




