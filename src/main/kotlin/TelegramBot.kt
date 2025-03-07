import dictionary.LearnWordsTrainer
import dictionary.Question
import dictionary.STATISTIC_TO_SEND

const val UPDATES_ID_TEXT_REGEX_PARAM = "\"update_id\":(\\d+).*?\"text\":\"(.+?)\""
const val MESSAGE_REGEX_PARAM = "\"chat\":\\{\"id\":(\\d+)"
const val CALLBACK_DATA_REGEX_PARAM = "\"data\":\"(.+?)\""

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    var updateId = 0
    var chatId: Long = 0
    var userMessage: String
    var question: Question? = null

    var newUpdates: String
    var lastUpdateMatch: MatchResult?
    var dataCallbackMatch: MatchResult?

    while (true) {
        Thread.sleep(2000)
        newUpdates = telegramBotService.getUpdates(updateId)
        println(newUpdates)

        lastUpdateMatch = getLastUpdateMatchResult(
            newUpdates, UPDATES_ID_TEXT_REGEX_PARAM.toRegex(RegexOption.DOT_MATCHES_ALL)
        )
        dataCallbackMatch = getLastUpdateMatchResult(
            newUpdates, CALLBACK_DATA_REGEX_PARAM.toRegex()
        )
        lastUpdateMatch?.groupValues?.let { values ->
            updateId = values.getOrNull(1)?.toIntOrNull()?.plus(1) ?: 0
            userMessage = values.getOrNull(2) ?: "no_user_message"

            println(userMessage)
            if (userMessage == "/menu") {
                if (chatId == 0L) {
                    val chatIdMatch = getLastUpdateMatchResult(
                        newUpdates, MESSAGE_REGEX_PARAM.toRegex()
                    )
                    chatId = getChatIdFromMatchResult(chatIdMatch)
                }
                telegramBotService.sendMainMenu(chatId)
            }
        }

        dataCallbackMatch?.groupValues?.let { values ->
            val dataCallbackString = values[1]
            when {
                CALLBACK_DATA_START_LEARNING_CLICKED == dataCallbackString ->
                    question = checkNextQuestionAndSend(
                        trainer,
                        telegramBotService,
                        chatId
                    )

                CALLBACK_DATA_STATISTICS_CLICKED == dataCallbackString -> {
                    val statistics = trainer.getStatistics()
                    telegramBotService.sendMessage(
                        chatId,
                        STATISTIC_TO_SEND.format(
                            statistics.learnedWordsCount,
                            statistics.totalWordsCount,
                            statistics.learnedWordsPercent
                        )
                    )
                }

                dataCallbackString.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    val correctAnswer = question?.correctAnswer
                    val isRightAnswer =
                        trainer.checkAnswer(dataCallbackString.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt())
                    if (isRightAnswer) {
                        telegramBotService.sendMessage(chatId, "Верно!")
                    } else {
                        telegramBotService.sendMessage(
                            chatId,
                            "Не верно! ${correctAnswer?.originalWord} - ${correctAnswer?.translatedWord}"
                        )
                    }

                    question = trainer.getNextQuestion()
                    question?.let {
                        telegramBotService.sendQuestion(chatId, it)
                    }
                }

                else -> ""
            }
        }


    }

}

private fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
): Question? {
    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId, "Вы выучили все слова в базе")
    } else {
        telegramBotService.sendQuestion(chatId, question)
    }
    return question
}

private fun getLastUpdateMatchResult(updates: String, updatesRegex: Regex): MatchResult? =
    updatesRegex.findAll(updates).lastOrNull()

private fun getChatIdFromMatchResult(match: MatchResult?): Long {
    val chatId = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    return chatId
}