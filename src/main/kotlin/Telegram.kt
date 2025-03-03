import dictionary.LearnWordsTrainer

const val UPDATES_ID_TEXT_REGEX_PARAM = "\"update_id\":(\\d+).*?\"text\":\"(.+?)\""
const val MESSAGE_REGEX_PARAM = "\"chat\":\\{\"id\":(\\d+)"
const val DATA_CALLBACK_REGEX_PARAM = "\"data\":\"(.+?)\""

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    var updateId = 0
    var chatId: Long = 0
    var userMessage: String
    var callbackData: String

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
            newUpdates, DATA_CALLBACK_REGEX_PARAM.toRegex()
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
            callbackData = values[1]
            when(callbackData){
                "statistics" -> {
                    val statistics = trainer.getStatistics()
                    telegramBotService.sendMessage(chatId,
                        "Выучено ${statistics.learnedWordsCount} из" +
                            " ${statistics.totalWordsCount} слов |" +
                            " ${statistics.learnedWordsPercent}%")
                }
                "startLearning" -> telegramBotService.sendMessage(chatId, "ещё не реализованно")
                else -> ""
            }
        }

    }

}

private fun getLastUpdateMatchResult(updates: String, updatesRegex: Regex): MatchResult? =
    updatesRegex.findAll(updates).lastOrNull()

private fun getChatIdFromMatchResult(match: MatchResult?): Long{
    val chatId = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    return chatId
}