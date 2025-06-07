package database.test
/*
import database.main.UserDictionaryRepository
import bot.Word
import java.io.File

/**
 * Тестовый репозиторий для тестирования функциональности изучения слов
 * Расширяет UserDictionaryRepository дополнительными методами для тестирования
 */
class TestDictionaryRepository(
    jdbcUrl: String,
    private val testUsername: String,
    private val testChatId: Long
) : UserDictionaryRepository(jdbcUrl, 3) {

    fun initTestTables() {
        withConnection { connection ->
            prepareTables(connection)
        }
    }

    /**
     * Очищает все данные из базы данных
     */
    fun clearData() {
        withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM user_answers")
                statement.executeUpdate("DELETE FROM users")
                statement.executeUpdate("DELETE FROM words")
            }
        }
    }

    /**
     * Вставляет тестовые данные из файла
     * @param file файл с тестовыми данными в формате "слово|перевод|количество_правильных_ответов"
     */
    fun insertTestData(file: File) {
        // Добавляем тестового пользователя
        addNewUser(testChatId, testUsername)

        val words = mutableListOf<Word>()
        val wordAnswers = mutableListOf<Word>()

        file.forEachLine { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                val originalWord = parts[0].trim()
                val translatedWord = parts[1].trim()
                val correctAnswersCount = if (parts.size >= 3) parts[2].trim().toIntOrNull() ?: 0 else 0

                val word = Word(originalWord, translatedWord, correctAnswersCount)
                words.add(word)
                wordAnswers.add(word)
            }
        }

        if (words.isNotEmpty()) {
            addWordsToUser(testChatId, words)
            addUserAnswersToUser(testChatId, wordAnswers)

            // Устанавливаем количество правильных ответов для каждого слова
            wordAnswers.forEach { word ->
                if (word.correctAnswersCount > 0) {
                    setUsersCorrectAnswersCount(testChatId, word.originalWord, word.correctAnswersCount)
                }
            }
        }
    }
}

 */