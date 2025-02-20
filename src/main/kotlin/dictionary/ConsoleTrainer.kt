package dictionary


fun main() {
    val trainer = LearnWordsTrainer()
    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")
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

fun startLearning(trainer: LearnWordsTrainer) {
    val inputRange = 0..trainer.questionWordsCount

    while (true) {
        val question = trainer.getNextQuestion()
        if (question == null) {
            println("Слова для изучения кончились")
            return
        }
        println(getQuestionString(question))

        val userInput = readln().toIntOrNull()
        if (userInput != null && userInput in inputRange) {
            when (userInput) {
                0 -> return
                else -> {
                    val resultText = "${question.correctAnswer.originalWord} - ${question.correctAnswer.translatedWord}"
                    if (trainer.checkAnswer(userInput - 1)) println("Верно! $resultText")
                    else  println("Не верно! $resultText")
                }
            }
        } else {
            println("Вводите числа равные вариантам ответа")
        }
    }
}

fun getQuestionString(question: Question): String =
    buildString{
        append("${question.correctAnswer.originalWord}:\n")
        append(
            question.variants
                .mapIndexed { index: Int, variant: Word -> "${index + 1} - ${variant.translatedWord}" }
                .joinToString(separator = "\n")
        )
        append("\n------------\n0 - Меню")
    }







