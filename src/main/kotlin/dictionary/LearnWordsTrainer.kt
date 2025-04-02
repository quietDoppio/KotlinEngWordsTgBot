package dictionary

const val HUNDRED_PERCENT = 100
const val WORDS_FILE_NAME = "words"

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
    val learnedWordsLimit: Int = 3,
    val questionWordsCount: Int = 4
) {
    private val fileUserDictionary = FileUserDictionary()

    private var _question: Question? = null
    val question get() = _question

    fun getNextQuestion(chatId: Long): Question? {
        val unlearnedList = fileUserDictionary.getUnlearnedWords(chatId)
            .shuffled()
            .take(questionWordsCount)

        if (unlearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = unlearnedList.firstOrNull { it != question?.correctAnswer } ?: unlearnedList.random()
            val variants = if (unlearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = fileUserDictionary.getLearnedWords(chatId)
                        .shuffled()
                        .take(questionWordsCount - unlearnedList.size)
                    addAll(unlearnedList + learnedWords)
                }.shuffled()
            } else {
                unlearnedList.shuffled().take(questionWordsCount)
            }

            _question = Question(
                variants = variants,
                correctAnswer = correctAnswer,
            )
            return question
        }
    }

    fun checkAnswer(inputIndex: Int, chatId: Long): Boolean {
        question?.let {
            val selectedWord = it.variants[inputIndex]
            val isRightAnswer = selectedWord.originalWord == it.correctAnswer.originalWord
            if (isRightAnswer) {
                val newCorrectAnswerCount = fileUserDictionary.getCurrentAnswerCount(selectedWord.originalWord, chatId) + 1
                fileUserDictionary.setCorrectAnswersCount(
                    selectedWord.originalWord,
                    newCorrectAnswerCount,
                    chatId
                )
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(chatId: Long): Statistic {
        val totalWordsCount = fileUserDictionary.getSize()
        val learnedWordsCount = fileUserDictionary.getNumOfLearnedWords(chatId)
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * HUNDRED_PERCENT) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }

    fun resetStatistics(chatId: Long) {
        fileUserDictionary.resetUserProgress(chatId)
    }

}


