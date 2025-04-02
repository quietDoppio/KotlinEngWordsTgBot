package dictionary

const val STATISTIC_TO_SEND = "Выучено %d из %d слов | %d%%"

fun main() {
    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")
        val userInput = readln()
        when (userInput) {
            "1" -> startLearning(trainer)
            "2" -> {
                val statistic = trainer.getStatistics(148)
                println(
                    STATISTIC_TO_SEND.format(
                        statistic.learnedWordsCount,
                        statistic.totalWordsCount,
                        statistic.learnedWordsPercent
                    )
                )
            }

            "0" -> return
            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun startLearning(trainer: LearnWordsTrainer) {
    val inputRange = 0..trainer.questionWordsCount

    while (true) {
        val question = trainer.getNextQuestion(148)
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
                    if (trainer.checkAnswer(userInput - 1, 148)) println("Верно! $resultText")
                    else println("Не верно! $resultText")
                }
            }
        } else {
            println("Вводите числа равные вариантам ответа")
        }
    }
}

fun getQuestionString(question: Question): String =
    buildString {
        append("${question.correctAnswer.originalWord}:\n")
        append(
            question.variants
                .mapIndexed { index: Int, variant: Word -> "${index + 1} - ${variant.translatedWord}" }
                .joinToString(separator = "\n")
        )
        append("\n------------\n0 - Меню")
    }







