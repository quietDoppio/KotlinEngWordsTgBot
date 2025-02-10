package dictionary

import java.io.File

const val HUNDRED_PERCENT = 100

data class Word(
    val originalWord: String,
    val translatedWord: String,
    val correctAnswerCount: Int = 0,
)

fun main() {
    val dictionary = loadDictionary()
    while (true) {
        println(
            """Меню:
            |1 - Учить слова
            |2 - Статистика
            |0 - Выход""".trimMargin()
        )

        val userInput = readln()
        when (userInput) {
            "1" -> println("Режим изучения слов")
            "2" -> {
                val totalWords = dictionary.size
                val learnedWords = dictionary.filter { it.correctAnswerCount >= 3 }.size
                val learnedWordsPercent =
                    if (learnedWords != 0) (learnedWords.toDouble() / totalWords) * HUNDRED_PERCENT else 0
                println("Выучено $learnedWords из $totalWords | ${learnedWordsPercent.toInt()}%\n")
            }

            "0" -> println("Выход")
            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(): List<Word> {
    val dictionary = mutableListOf<Word>()
    val wordsFile = File("words")
    wordsFile.createNewFile()

    wordsFile.forEachLine {
        val line = it.split("|")
        dictionary.add(
            Word(
                originalWord = line[0],
                translatedWord = line[1],
                correctAnswerCount = line.getOrNull(2)?.toIntOrNull() ?: 0
            )
        )
    }
    return dictionary
}
