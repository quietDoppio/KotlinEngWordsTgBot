package dictionary

import java.io.File

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
            "2" -> println("Режим просмотра статистика")
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
