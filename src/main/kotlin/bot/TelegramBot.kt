package bot

import database.main.UserDictionaryRepository

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer(UserDictionaryRepository("${Constants.JDBC_URL}Database.db", 3))
    val botUpdateProcessor = BotUpdateProcessor(telegramBotService, trainer)

    botUpdateProcessor.run()
}