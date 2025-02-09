package dictionary

import java.io.File

data class Word(
    val originalWord: String,
    val translatedWord: String,
    val correctAnswerCount: Int = 0,
)

fun main() {
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

    dictionary.forEach { println(it) }
}
