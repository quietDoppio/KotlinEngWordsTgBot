package dictionary

import java.io.File

const val HUNDRED_PERCENT = 100
const val LEARN_WORD_LIMIT = 3
const val QUESTION_WORDS_COUNT = 4

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
            "1" -> startLearnWords(dictionary)
            "2" -> println(getStatistics(dictionary))
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

fun getStatistics(dictionary: List<Word>): String {
    val totalWords = dictionary.size
    val learnedWordsCount = dictionary.filter { it.correctAnswerCount >= LEARN_WORD_LIMIT }.size
    val learnedWordsPercent =
        if (learnedWordsCount != 0) ((learnedWordsCount.toDouble() / totalWords) * HUNDRED_PERCENT).toInt() else 0
    val statistics = "Выучено $learnedWordsCount из $totalWords слов | ${learnedWordsPercent}%\n"
    return statistics
}

fun startLearnWords(dictionary: List<Word>) {
    val notLearnedList = dictionary.filter { it.correctAnswerCount < LEARN_WORD_LIMIT }
    while (true) {
        if (notLearnedList.isEmpty()) {
            println("Все слова в словаре выучены")
            return
        }

        val questionWords = notLearnedList.shuffled().take(QUESTION_WORDS_COUNT)
        if (questionWords.size < QUESTION_WORDS_COUNT) {
            println(
                "Недостаточно слов для изучения\n" +
                        "В данный момент невыученных слов - ${notLearnedList.size} из необходимых $QUESTION_WORDS_COUNT\n"
            )
            return
        }

        val wordToGuess = questionWords.random().originalWord
        println(
            """$wordToGuess:
            |1 - ${questionWords[0].translatedWord}
            |2 - ${questionWords[1].translatedWord}
            |3 - ${questionWords[2].translatedWord}
            |4 - ${questionWords[3].translatedWord}
            """.trimMargin()
        )
        val userInput = readln().toIntOrNull() ?: 0
        if (userInput in 1..QUESTION_WORDS_COUNT) {
            val isRightAnswer = questionWords[userInput - 1].originalWord == wordToGuess
            if (isRightAnswer) println("Верно! $wordToGuess - ${questionWords[userInput - 1].translatedWord}")
            else println("Не верно! $wordToGuess не ${questionWords[userInput - 1].translatedWord}")
        } else {
            println("Введите число 1, 2, 3 или 4")
        }
    }
}
