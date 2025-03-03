const val UPDATES_ID_TEXT_REGEX_PARAM = "\"update_id\":(\\d+).*?\"text\":\"(.+?)\""
const val MESSAGE_REGEX_PARAM = "\"chat\":\\{\"id\":(\\d+)"

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)

    var updateId = 0
    var chatId = 0
    var userMessage: String

    var newUpdates: String
    var lastUpdateMatch: MatchResult?

    while (true) {
        Thread.sleep(2000)
        newUpdates = telegramBotService.getUpdates(updateId)
        lastUpdateMatch = getLastUpdateMatchResult(
            newUpdates, UPDATES_ID_TEXT_REGEX_PARAM.toRegex(RegexOption.DOT_MATCHES_ALL)
        )

        lastUpdateMatch?.groupValues?.let { values ->
            updateId = values.getOrNull(1)?.toIntOrNull()?.plus(1) ?: 0
            userMessage = values.getOrNull(2) ?: "nothing"
            println(userMessage)
            if (userMessage == "/start") {
                if (chatId == 0) {
                    val chatIdMatch = getLastUpdateMatchResult(
                        newUpdates, MESSAGE_REGEX_PARAM.toRegex()
                    )
                    chatId = getChatIdFromMatchResult(chatIdMatch)
                }
                telegramBotService.sendMessage(chatId, "Привет, мир!")
            }
        }
    }

}

private fun getLastUpdateMatchResult(updates: String, updatesRegex: Regex): MatchResult? =
    updatesRegex.findAll(updates).lastOrNull()

private fun getChatIdFromMatchResult(match: MatchResult?): Int{
    val chatId = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return chatId
}