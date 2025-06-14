package bot

import NEW.TelegramFilesHelper
import NEW.TelegramRequestSender
import NEW.TelegramUpdateSource
import database.main.UserDictionaryRepository

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramApiService = TelegramApiService(botToken)
    val trainer = LearnWordsTrainer(UserDictionaryRepository("${Constants.JDBC_URL}Database.db", 3))
    val botUpdateProcessor = BotUpdateProcessor(
        TelegramUpdateSource(telegramApiService),
        TelegramRequestSender(telegramApiService),
        TelegramFilesHelper(telegramApiService),
        trainer
    )
    botUpdateProcessor.run()
}