package config

object BotConfig {

    object Database {
        const val JDBC_URL = "jdbc:sqlite:"
        const val DATABASE_FILE = "Database.db"
        const val FULL_DATABASE_PATH = "$JDBC_URL$DATABASE_FILE"
    }

    object Learning {
        const val MIN_CORRECT_ANSWERS = 3
        const val QUESTION_VARIANTS_COUNT = 4
        const val POLLING_INTERVAL_MS = 2000L
    }

    object Files {
        const val COPY_BUFFER_SIZE = 16 * 1024
        const val IMAGES_PATH = "build/libs"
        const val WORDS_FILE = "words.txt"
    }

    object Messages {
        const val STATISTIC_FORMAT = "Выучено %d из %d слов | %d%%"
        const val GO_BACK_MESSAGE = "Назад"
    }

    object App {
        const val UNKNOWN_USER = "unknown_user"
    }


    object Telegram {
        const val API_BASE_URL = "https://api.telegram.org/bot"

        object Emojis {
            const val CHECKMARK = "\u2705"
            const val CROSSMARK = "\u274C"
            const val HUNDRED = "\uD83D\uDCAF"
            const val ENG_FLAG = "\uD83C\uDDFA\uD83C\uDDF8"
            const val RU_FLAG = "\uD83C\uDDF7\uD83C\uDDFA"
            const val DIGITS = "\uFE0F\u20E3"
        }

        object KeyWords{
            const val START = "/start"
            const val INFO = "/info"
        }

        object Callbacks {
            const val GO_BACK_WORD_MENU_CLICKED = "GO_BACK_WORD_MENU_CLICKED"
            const val GO_BACK_SELECT_WORD_SESSION_CLICKED = "GO_BACK_SELECT_WORD_SESSION_CLICKED"
            const val SET_PICTURE_SESSION_CLICKED = "SET_PICTURE_SESSION_CLICKED"
            const val DELETE_WORD_CLICKED = "DELETE_WORD_CLICKED"
            const val EDIT_WORD_SESSION_CLICKED = "EDIT_WORD_SESSION_CLICKED"
            const val STATISTICS_CLICKED = "STATISTICS_CLICKED"
            const val START_LEARNING_CLICKED = "START_LEARNING_CLICKED"
            const val RESET_CLICKED = "RESET_CLICKED"
            const val RETURN_MAIN_MENU_CLICKED = "RETURN_MAIN_MENU_CLICKED"
            const val ADD_WORDS_SESSION_CLICKED = "ADD_WORDS_SESSION_CLICKED"
            const val SELECT_WORD_SESSION_CLICKED = "SELECT_WORD_SESSION_CLICKED"
            const val DELETE_DICTIONARY_CLICKED = "DELETE_DICTIONARY_CLICKED"
            const val ANSWER_PREFIX = "answer_" }
    }
}
