package dictionary

import java.io.File

const val HUNDRED_PERCENT = 100

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

class LearnWordsTrainer(
    private val learnedWordsLimit: Int = 3,
    val questionWordsCount: Int = 4
) {
    val fileUserDictionary: FileUserDictionary = FileUserDictionary()
    private var _question: Question? = null
    val question get() = _question


    fun getNextQuestion(): Question? {
        val notLearnedList = fileUserDictionary.getUnlearnedWords()
            .shuffled()
            .take(questionWordsCount)

        if (notLearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = notLearnedList.firstOrNull { it != question?.correctAnswer } ?: notLearnedList.random()
            val variants = if (notLearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = fileUserDictionary.getLearnedWords()
                        .shuffled()
                        .take(questionWordsCount - notLearnedList.size)
                    addAll(notLearnedList + learnedWords)
                }.shuffled()
            } else {
                notLearnedList.shuffled().take(questionWordsCount)
            }

            _question = Question(
                variants = variants,
                correctAnswer = correctAnswer,
            )
            return question
        }
    }

    fun checkAnswer(inputIndex: Int): Boolean {
        question?.let {
            val selectedWord = it.variants[inputIndex]
            val isRightAnswer = selectedWord.originalWord == it.correctAnswer.originalWord
            if (isRightAnswer) {
                fileUserDictionary.setCorrectAnswersCount(selectedWord.originalWord, selectedWord.correctAnswerCount + 1)
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(): Statistic {
        val totalWordsCount = fileUserDictionary.getSize()
        val learnedWordsCount = fileUserDictionary.getLearnedWords().count()
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * HUNDRED_PERCENT) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }
    fun resetStatistics(){
        fileUserDictionary.resetUserProgress()
    }

}


