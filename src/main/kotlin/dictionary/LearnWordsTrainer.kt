package dictionary

import java.io.File

const val HUNDRED_PERCENT = 100
const val LEARN_WORD_LIMIT = 3
const val WORDS_FILE_NAME = "words"

data class Word(
    val originalWord: String,
    val translatedWord: String,
    var correctAnswerCount: Int = 0,
) {
    override fun toString(): String {
        return "$originalWord|$translatedWord|$correctAnswerCount"
    }
}

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

data class Statistic(
    val totalWordsCount: Int,
    val learnedWordsCount: Int,
    val learnedWordsPercent: Int,
)

class LearnWordsTrainer() {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    private fun loadDictionary(): MutableList<Word> {
        val dictionary = mutableListOf<Word>()
        val wordsFile = File(WORDS_FILE_NAME)

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

    private fun saveDirectory(changedWord: Word) {
        val wordsFile = File(WORDS_FILE_NAME)
        val newString = changedWord.toString()
        val oldString = wordsFile.readLines().first { fileString ->
            fileString.dropLast(1) == newString.dropLast(1)
        }

        val content = wordsFile.readText()
        val updatedContent = content.replace(oldString, newString)
        wordsFile.writeText(updatedContent)
    }


    fun getNextQuestion(questionWordsCount: Int): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswerCount < LEARN_WORD_LIMIT }
        if (notLearnedList.isEmpty()) {
            return null
        } else {
            val variants = notLearnedList.take(questionWordsCount).toMutableSet()
            val correctAnswer =
                if(question != null && variants.size != 1) variants.filter { it != question!!.correctAnswer }.random()
                else variants.random()

            if(variants.size < questionWordsCount){
                val count = questionWordsCount - variants.size
                val additionalWords = (dictionary - variants).shuffled().take(count)
                variants.addAll(additionalWords)
            }
            question = Question(
                variants = variants.shuffled(),
                correctAnswer = correctAnswer,
            )
            return question
        }
    }

    fun checkAnswer(userAnswerInput: Int): Boolean {
        question!!.let{
            val selectedWord = it.variants[userAnswerInput]
            val isRightAnswer = selectedWord.originalWord == it.correctAnswer.originalWord
            if (isRightAnswer) {
                val index = dictionary.indexOf(selectedWord)
                dictionary[index].correctAnswerCount += 1
                saveDirectory(dictionary[index])
            }
            return isRightAnswer
        }
    }

    fun getStatistics(): Statistic {
        val totalWordsCount = dictionary.size
        val learnedWordsCount = dictionary.filter { it.correctAnswerCount >= LEARN_WORD_LIMIT }.size
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * HUNDRED_PERCENT) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }

}


