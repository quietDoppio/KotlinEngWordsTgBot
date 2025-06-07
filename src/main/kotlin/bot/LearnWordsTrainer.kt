package bot

import config.BotConfig
import database.TablesHandler
import database.main.UserRepository
import database.main.WordRepository

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

class LearnWordsTrainer(
    val userRepository: UserRepository,
    val wordRepository: WordRepository,
    val tablesHandler: TablesHandler,
    var questionWordsCount: Int = BotConfig.Learning.QUESTION_VARIANTS_COUNT,
) {
    init {
        tablesHandler.initTables()
    }

    private var _question: Question? = null
    val question get() = _question

    fun deleteAllWords(chatId: Long){
        userRepository.clearUsersWords(chatId)
    }

    fun getNextQuestion(chatId: Long): Question? {
        val unlearnedList = userRepository.getUnlearnedWords(chatId).shuffled().take(questionWordsCount)

        if (unlearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = unlearnedList.firstOrNull { it != question?.correctAnswer } ?: unlearnedList.random()
            val variants = if (unlearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = userRepository.getLearnedWords(chatId).shuffled()
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
                    userRepository.getUsersCurrentAnswerCount(chatId, selectedWord.originalWord) + 1

                userRepository.setUsersCorrectAnswersCount(
                    chatId, selectedWord.originalWord, newCorrectAnswerCount,
                )
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(chatId: Long): Statistic {
        val totalWordsCount = userRepository.getUsersWordsCount(chatId)
        val learnedWordsCount = userRepository.getUsersNumOfLearnedWords(chatId)
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * 100) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }

    fun resetStatistics(chatId: Long): Boolean = userRepository.resetUserProgress(chatId)
}
