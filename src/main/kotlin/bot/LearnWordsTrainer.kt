package bot

import config.BotConfig.Learning.QUESTION_VARIANTS_COUNT
import database.DataBaseRepository

class LearnWordsTrainer(
    private val repository: DataBaseRepository,
    private var questionWordsCount: Int = QUESTION_VARIANTS_COUNT,
) {
    private var _question: Question? = null
    val question get() = _question

    fun getNextQuestion(chatId: Long): Question? {
        val unlearnedList = repository.getUnlearnedWords(chatId).shuffled().take(questionWordsCount)

        if (unlearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = unlearnedList.firstOrNull { it != question?.correctAnswer } ?: unlearnedList.random()
            val variants = if (unlearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = repository.getLearnedWords(chatId).shuffled()
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
                    repository.getUsersCurrentAnswerCount(chatId, selectedWord.originalWord) + 1

                repository.setUsersCorrectAnswersCount(
                    chatId, selectedWord.originalWord, newCorrectAnswerCount,
                )
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(chatId: Long): Statistic {
        val totalWordsCount = repository.getUsersWordsCount(chatId)
        val learnedWordsCount = repository.getUsersNumOfLearnedWords(chatId)
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * 100) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }

    fun resetStatistics(chatId: Long): Boolean = repository.resetUserProgress(chatId)
}

data class Word(
    val originalWord: String,
    val translatedWord: String,
    val correctAnswersCount: Int = 0,
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