package dictionary

fun main() {
    val trainer = LearnWordsTrainer()
    while (true) {
        printMenu()
        val userInput = readln()
        when (userInput) {
            "1" -> startLearning(trainer)
            "2" -> {
                val statistic = trainer.getStatistics()
                println("Выучено ${statistic.learnedWordsCount} из ${statistic.totalWordsCount} слов | ${statistic.learnedWordsPercent}%")
            }
            "0" -> return
            else -> println("Введите число 1, 2 или 0")
        }
    }

}

fun printMenu() = println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")
fun startLearning(trainer: LearnWordsTrainer) {
    val inputRange = 0..QUESTION_WORDS_COUNT
    while (true) {
        val question = trainer.getNextQuestion()!!
        println(getQuestionString(question))

        val userInput = readln().toIntOrNull()
        if (userInput != null && userInput in inputRange) {

            when (userInput) {
                0 -> return
                else -> {
                    if (trainer.checkAnswer(userInput.minus(1))) {
                        println("Верно! ${question.correctAnswer.originalWord} - ${question.correctAnswer.translatedWord}")
                    } else {
                        println("Не верно! ${question.correctAnswer.originalWord} - ${question.variants[userInput.minus(1)].translatedWord}")
                    }
                }
            }

        } else {
            println("Введите число 1, 2, 3 или 4")
        }
    }
}

fun getQuestionString(question: Question): String {
    var questionString = "${question.correctAnswer.originalWord}:\n"
    questionString = questionString + question.variants
        .mapIndexed { index: Int, variant: Word -> "${index + 1} - ${variant.translatedWord}" }
        .joinToString(separator = "\n")

    return questionString
}






