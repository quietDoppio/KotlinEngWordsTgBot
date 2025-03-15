package dictionary

import java.io.File

const val HUNDRED_PERCENT = 100
const val WORDS_FILE_NAME = "words.txt"

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

class LearnWordsTrainer(private val fileName: String = WORDS_FILE_NAME, val learnedWordsLimit: Int = 3, val questionWordsCount: Int = 4) {
    private var _question: Question? = null
    val question get() = _question

    private val dictionary = loadDictionary()

    private fun loadDictionary(): MutableList<Word> {
        try {
        val dictionary = mutableListOf<Word>()
        val wordsFile = File("$fileName.txt")
        if(!wordsFile.exists()){
            File(WORDS_FILE_NAME).copyTo(wordsFile)
        }
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
        } catch (e: Exception){
            throw IndexOutOfBoundsException("Не удалось загрузить словарь")
        }
    }

    private fun saveDirectory(changedWord: Word) {
        val wordsFile = File("$fileName.txt")
        val newString = changedWord.toString()
        val oldString = wordsFile.readLines().first { fileString ->
            fileString.dropLast(1) == newString.dropLast(1)
        }

        val content = wordsFile.readText()
        val updatedContent = content.replace(oldString, newString)
        wordsFile.writeText(updatedContent)
    }


    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary
            .filter { it.correctAnswerCount < learnedWordsLimit }
            .shuffled()
            .take(questionWordsCount)

        if (notLearnedList.isEmpty()) {
            return null
        } else {
            val correctAnswer = notLearnedList.firstOrNull { it != question?.correctAnswer } ?: notLearnedList.random()
            val variants = if (notLearnedList.size < questionWordsCount) {
                buildList {
                    val learnedWords = dictionary
                        .filter { it.correctAnswerCount >= learnedWordsLimit }
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
                val index = dictionary.indexOf(selectedWord)
                dictionary[index].correctAnswerCount += 1
                saveDirectory(dictionary[index])
            }
            return isRightAnswer
        } ?: return false
    }

    fun getStatistics(): Statistic {
        val totalWordsCount = dictionary.size
        val learnedWordsCount = dictionary.filter { it.correctAnswerCount >= learnedWordsLimit }.size
        val learnedWordsPercent =
            if (learnedWordsCount != 0) ((learnedWordsCount * HUNDRED_PERCENT) / totalWordsCount) else 0

        return Statistic(
            totalWordsCount = totalWordsCount,
            learnedWordsCount = learnedWordsCount,
            learnedWordsPercent = learnedWordsPercent,
        )
    }
    fun resetStatistics(){
        dictionary.forEach { word ->
            word.correctAnswerCount = 0
            saveDirectory(word)
        }
    }

}


