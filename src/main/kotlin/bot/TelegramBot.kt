package bot

import Utils.TelegramFilesHelperImpl
import Utils.TelegramMessengerImpl
import Utils.TelegramUpdateSourceImpl
import api.TelegramApiService
import database.main.UserDictionaryRepository

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val apiService = TelegramApiService(botToken)
    val trainer = LearnWordsTrainer(UserDictionaryRepository("${Constants.JDBC_URL}Database.db", 3))
    val botUpdateProcessor = BotUpdateProcessor(
        TelegramUpdateSourceImpl(apiService),
        TelegramMessengerImpl(apiService),
        TelegramFilesHelperImpl(apiService),
        trainer
    )
    botUpdateProcessor.run()
}