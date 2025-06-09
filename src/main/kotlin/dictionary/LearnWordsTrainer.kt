package dictionary

import database.BaseJdbcRepository
import database.main.UserDictionaryRepository

const val HUNDRED_PERCENT = 100
const val JDBC_URL = "jdbc:sqlite:Database.db"

data class Word(
    val originalWord: String,
    val translatedWord: String,
)

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
    learnedWordsLimit: Int = 3, var questionWordsCount: Int = 4, val jdbcUrl: String = JDBC_URL
) {
    val dataService: BaseJdbcRepository = UserDictionaryRepository(jdbcUrl, learnedWordsLimit)

    private var _question: Question? = null
    val question get() = _question

    fun getNextQuestion(chatId: Long): Question? {
        val unlearnedList = dataService.getUnlearnedWords(chatId).shuffled().take(questionWordsCount)

        if (unlearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = unlearnedList.firstOrNull { it != question?.correctAnswer } ?: unlearnedList.random()
            val variants = if (unlearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = dataService.getLearnedWords(chatId).shuffled()
                        .take(questionWordsCount - unlearnedList.size)
                    addAll(unlearnedList + learnedWords)
                }.shuffled()
            } else {
                unlearnedList.shuffled().take(questionWordsCount)
            }

            _question = Question(variants = variants, correctAnswer = correctAnswer)

            return question
        }
    }

    fun checkAnswer(inputIndex: Int, chatId: Long): Boolean {
        question?.let {
            val selectedWord = it.variants[inputIndex]
            val isRightAnswer = selectedWord.originalWord == it.correctAnswer.originalWord
            if (isRightAnswer) {
                val newCorrectAnswerCount =
                    dataService.getCurrentAnswerCount(selectedWord.originalWord, chatId) + 1

                dataService.setCorrectAnswersCount(
                    chatId, selectedWord.originalWord, newCorrectAnswerCount,
                )
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(chatId: Long): Statistic {
        val totalWordsCount = dataService.getWordsCount(chatId)
        val learnedWordsCount = dataService.getNumOfLearnedWords(chatId)
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * HUNDRED_PERCENT) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }

    fun resetStatistics(chatId: Long): Boolean = dataService.resetUserProgress(chatId)
}


