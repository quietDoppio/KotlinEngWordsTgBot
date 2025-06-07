package bot

import utils.FilesHelper
import utils.TelegramMessenger
import utils.UpdateSource
import api.TelegramApiService
import bot.utils.AddWordsSessionHandler
import config.BotConfig
import database.ConnectionProvider
import database.TablesHandler
import database.main.UserRepository
import database.main.WordRepository

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val apiService = TelegramApiService(botToken)
    val connectionProvider = ConnectionProvider(BotConfig.Database.FULL_DATABASE_PATH)
    val trainer = LearnWordsTrainer(
        UserRepository(connectionProvider, BotConfig.Learning.MIN_CORRECT_ANSWERS),
        WordRepository(connectionProvider),
        TablesHandler(connectionProvider)
    )

    val botUpdateProcessor = BotUpdateProcessor(
        UpdateSource(apiService),
        TelegramMessenger(apiService),
        FilesHelper(apiService,trainer),
        trainer,
    )

    botUpdateProcessor.run()
}