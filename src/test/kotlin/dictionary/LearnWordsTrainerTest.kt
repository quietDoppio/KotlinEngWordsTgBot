package dictionary

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class LearnWordsTrainerTest {
    private companion object {
        private lateinit var trainer: LearnWordsTrainer
        
        @BeforeAll
        @JvmStatic
        fun initTrainer() {
            trainer = LearnWordsTrainer(jdbcUrl = "jdbc:sqlite:testDatabase.db")
        }
    }

    @BeforeEach
    fun setup() {
        trainer.fileUserDictionary.deleteData()
        if (trainer.questionWordsCount != 4) trainer.questionWordsCount = 4
    }

   private fun createTestTempFile(text: String): File {
        return kotlin.io.path.createTempFile(prefix = "tempFile", suffix = ".txt").toFile().apply {
            writeText(text)
            deleteOnExit()
        }
    }

    @Test
    fun tstStatisticsWith4WordsOf7() {
        val text = """
            define|определять|0
            solve|решать|0
            serve|служить|0
            empty|пустой|3
            full|полный|3
            narrow|узкий|3
            wide|широкий|3
        """.trimIndent()
        val file = createTestTempFile(text)
        trainer.fileUserDictionary.insertTestData(file)
        kotlin.test.assertEquals(
            Statistic(learnedWordsCount = 4, totalWordsCount = 7, learnedWordsPercent = 57),
            trainer.getStatistics(0L),
        )
    }

    @Test
    fun testStatisticsWithCorruptedFile() {
        val text = """  
            222|}
            0|22 ||| 22|          
            |
        """.trimIndent()
        val corruptedFile = createTestTempFile(text)
        trainer.fileUserDictionary.insertTestData(corruptedFile)
        kotlin.test.assertEquals(
            Statistic(learnedWordsCount = 0, totalWordsCount = 0, learnedWordsPercent = 0), trainer.getStatistics(0L)
        )
    }

    @Test
    fun testNextQuestionWith5UnlearnedWords() {
        trainer.questionWordsCount = 5
        val text = """
            word1|trans1|1
            word2|trans2|1
            word3|trans3|1
            word4|trans4|1
            word5|trans5|1
            word6|trans6|3
            word7|trans7|3
        """.trimIndent()
        val unlearnedWordsFile = createTestTempFile(text)

        trainer.fileUserDictionary.insertTestData(unlearnedWordsFile)
        val numOfUnlearnedWords = trainer.fileUserDictionary.getNumOfUnlearnedWords(0L)
        val question = trainer.getNextQuestion(0L)

        kotlin.test.assertEquals(5, numOfUnlearnedWords, "unlearned words")
        kotlin.test.assertEquals(5, question?.variants?.size, "variants list size")
        kotlin.test.assertEquals(
            true,
            question?.variants?.size == numOfUnlearnedWords,
            "questionWordsSize: ${question?.variants?.size}\nactualUnlearnedWordsSize: ${numOfUnlearnedWords}"
        )
    }


    @Test
    fun getNextQuestionWithAllWordsLearned() {
        val text = """
                girl|девочка|3
                boy|мальчик|3
                man|мужчина|3
                woman|женщина|3
                grandmother|бабушка|3
            """.trimIndent()
        val learnedWordsFile = createTestTempFile(text)

        trainer.fileUserDictionary.insertTestData(learnedWordsFile)
        val question = trainer.getNextQuestion(0L)
        val numOfWords = trainer.fileUserDictionary.getSize(0L)
        val numOfLearnedWords = trainer.fileUserDictionary.getNumOfLearnedWords(0L)
        kotlin.test.assertNull(question, "question is null")
        kotlin.test.assertEquals(numOfWords, numOfLearnedWords, "not every word is learned")
    }

    @Test
    fun checkAnswerTrue() {
        val text = """
            girl|девочка|0
            boy|мальчик|0
            man|мужчина|0
            woman|женщина|0
        """.trimIndent()
        val checkAnswerFile = createTestTempFile(text)

        trainer.fileUserDictionary.insertTestData(checkAnswerFile)
        val question = trainer.getNextQuestion(0L)
        kotlin.test.assertNotNull(question, "question is null")

        val correctAnswer = question.correctAnswer
        val indexOfCorrect = question.variants.indexOf(correctAnswer)
        kotlin.test.assertEquals(
            true, trainer.checkAnswer(indexOfCorrect, 0L), """
                    correctAnswer: ${correctAnswer.originalWord} - $indexOfCorrect 
                    variants: ${
                question.variants.mapIndexed { i, word -> "${word.originalWord} - $i" }.joinToString(", ")
            }
                """.trimIndent()
        )
        kotlin.test.assertEquals(
            1,
            trainer.fileUserDictionary.getCurrentAnswerCount(
                correctAnswer.originalWord, 0L
            ),
            "correctAnswerCount isnt increased"
        )

    }


    @Test
    fun checkAnswerFalse() {
        val text = """
                girl|девочка|0
                boy|мальчик|0
                man|мужчина|0
                woman|женщина|0
            """.trimIndent()
        val checkAnswerFile = createTestTempFile(text)

        trainer.fileUserDictionary.insertTestData(checkAnswerFile)
        val question = trainer.getNextQuestion(0L)
        question?.let {
            val correctAnswer = it.correctAnswer
            val indexOfCorrect = it.variants.indexOf(correctAnswer)
            val indexOfIncorrect = it.variants.indexOfFirst { word -> word != correctAnswer }
            kotlin.test.assertEquals(
                false, trainer.checkAnswer(indexOfIncorrect, 0L), """
                    correctAnswer: ${correctAnswer.originalWord} - $indexOfCorrect 
                    variants: ${it.variants.mapIndexed { i, word -> "${word.originalWord} - $i" }.joinToString(", ")}
                """.trimIndent()
            )
        }
    }

    @Test
    fun resetProgressWith2WordsInDictionary() {
        val text = """
                girl|девочка|3
                boy|мальчик|3
            """.trimIndent()
        val file = createTestTempFile(text)

        trainer.fileUserDictionary.insertTestData(file)

        kotlin.test.assertEquals(2, trainer.fileUserDictionary.getNumOfLearnedWords(0L))
        kotlin.test.assertEquals(true, trainer.resetStatistics(0L))
        kotlin.test.assertEquals(0, trainer.fileUserDictionary.getNumOfLearnedWords(0L))
    }
}