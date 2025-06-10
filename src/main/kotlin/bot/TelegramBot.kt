package bot

import database.main.UserDictionaryRepository
import dictionary.LearnWordsTrainer
import dictionary.Question
import deprecated.STATISTIC_TO_SEND
import dictionary.JDBC_URL
import dictionary.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    val botToken: String = args[0]
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer(UserDictionaryRepository(JDBC_URL, 3))
    val botUpdateProcessor = BotUpdateProcessor(telegramBotService, trainer)

    botUpdateProcessor.run()
}