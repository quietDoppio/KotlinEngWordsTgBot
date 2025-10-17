package bot

import utils.FilesHelper
import api.TelegramApiService
import config.BotConfig.Database.FULL_DATABASE_PATH
import database.ConnectionProvider
import database.DataBaseRepository
import kotlinx.coroutines.runBlocking
import utils.TelegramMessenger

fun main(args: Array<String>): Unit = runBlocking {
    val botToken = System.getenv("BOT_TOKEN") ?: args.getOrNull(0) ?: error("Нет бот токена")
    val apiService = TelegramApiService(botToken)

    val repository = DataBaseRepository(ConnectionProvider(FULL_DATABASE_PATH)).also {
        it.initTables()
    }

    val filesHelper = FilesHelper(apiService, repository)
    val trainer = LearnWordsTrainer(repository)

    val idsStorage = IdsStorage()
    val dataStorage = UserBotDataStorage(idsStorage, SessionStorage(), WordsStorage())
    val messenger = TelegramMessenger(apiService, idsStorage)

    val botController =
        BotController(repository, trainer, apiService, dataStorage, filesHelper, messenger)
    val sessionHandler =
        SessionHandler(apiService, repository, dataStorage, messenger, botController::showWordEditorMenu)

    BotUpdateProcessor(dataStorage, sessionHandler, botController, apiService::getUpdates, repository::addNewUser)
        .run()
}