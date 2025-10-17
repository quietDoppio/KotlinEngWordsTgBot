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
        val pictureEnds = setOf(".jpg", ".png")
    }

    object App {
        const val UNKNOWN_USER = "unknown_user"
    }
}
