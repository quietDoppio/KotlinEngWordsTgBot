import config.BotConfig

/**
 * @deprecated Используйте BotConfig вместо Constants
 * Константы перенесены в config.BotConfig для лучшей организации
 */
@Deprecated("Use BotConfig instead", ReplaceWith("BotConfig"))
object Constants {
    @Deprecated("Use BotConfig.Telegram.Emojis.CHECKMARK")
    const val EMOJI_CHECKMARK = "\u2705"
    
    @Deprecated("Use BotConfig.Telegram.Emojis.CROSSMARK")
    const val EMOJI_CROSSMARK = "\u274C"
    
    @Deprecated("Use BotConfig.Telegram.Emojis.HUNDRED")
    const val EMOJI_HUNDRED = "\uD83D\uDCAF"
    
    @Deprecated("Use BotConfig.Telegram.Emojis.ENG_FLAG")
    const val EMOJI_ENG_FLAG = "\uD83C\uDDFA\uD83C\uDDF8"
    
    @Deprecated("Use BotConfig.Telegram.Emojis.RU_FLAG")
    const val EMOJI_RU_FLAG = "\uD83C\uDDF7\uD83C\uDDFA"
    
    @Deprecated("Use BotConfig.Telegram.Emojis.DIGITS")
    const val EMOJI_DIGITS = "\uFE0F\u20E3"

    @Deprecated("Use BotConfig.Telegram.API_BASE_URL")
    const val API_TELEGRAM_URL = "https://api.telegram.org/bot"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.STATISTICS_CLICKED")
    const val CALLBACK_DATA_STATISTICS_CLICKED = "DATA_CALLBACK_STATISTICS_CLICKED"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.START_LEARNING_CLICKED")
    const val CALLBACK_DATA_START_LEARNING_CLICKED = "DATA_CALLBACK_START_LEARNING_CLICKED"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.RESET_CLICKED")
    const val CALLBACK_DATA_RESET_CLICKED = "bot.CALLBACK_DATA_RESET_CLICKED"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.RETURN_CLICKED")
    const val CALLBACK_DATA_RETURN_CLICKED = "bot.CALLBACK_DATA_RETURN_CLICKED"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.ADD_WORDS")
    const val CALLBACK_DATA_ADD_WORDS = "bot.CALLBACK_DATA_ADD_WORDS"
    
    @Deprecated("Use BotConfig.Telegram.CallbackData.ANSWER_PREFIX")
    const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

    @Deprecated("Use BotConfig.Database.JDBC_URL")
    const val JDBC_URL = "jdbc:sqlite:"

    @Deprecated("Use BotConfig.App.HUNDRED_PERCENT")
    const val HUNDRED_PERCENT = 100
}

/**
 * @deprecated Используйте BotConfig.Learning вместо LearningConfig
 * Конфигурация обучения перенесена в config.BotConfig.Learning
 */
@Deprecated("Use BotConfig.Learning instead", ReplaceWith("BotConfig.Learning"))
object LearningConfig {
    @Deprecated("Use BotConfig.Learning.MIN_CORRECT_ANSWERS")
    const val MIN_CORRECT_ANSWERS = 3
    
    @Deprecated("Use BotConfig.Learning.QUESTION_VARIANTS_COUNT")
    const val QUESTION_VARIANTS_COUNT = 4
    
    @Deprecated("Use BotConfig.Learning.POLLING_INTERVAL_MS")
    const val POLLING_INTERVAL_MS = 2000L
    
    @Deprecated("Use BotConfig.Files.COPY_BUFFER_SIZE")
    const val FILE_COPY_BUFFER_SIZE = 16 * 1024
    
    @Deprecated("Use BotConfig.Files.PHOTO_WIDTH_FOR_FILE_ID")
    const val PHOTO_WIDTH_FOR_FILE_ID = 320
}