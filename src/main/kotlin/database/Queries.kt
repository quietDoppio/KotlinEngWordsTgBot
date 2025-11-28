package database

object Queries {
    const val CREATE_TABLE_WORDS = """
        CREATE TABLE IF NOT EXISTS words (
        id INTEGER PRIMARY KEY,      
        user_id INTEGER,
        text VARCHAR,
        translate VARCHAR,
        file_id VARCHAR DEFAULT NULL,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
        UNIQUE (user_id, text)
        )
    """
    const val CREATE_TABLE_USERS = """
        CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
        chat_id INTEGER UNIQUE,
        user_name VARCHAR
        )
    """
    const val CREATE_TABLE_USERS_ANSWERS = """
        CREATE TABLE IF NOT EXISTS user_answers (
        user_id INTEGER,
        word_id INTEGER,
        correct_answer_count INTEGER DEFAULT 0,
        PRIMARY KEY (user_id, word_id),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
        FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE
        )
    """
    const val UPDATE_WORD = "UPDATE OR IGNORE words SET text = ?, translate = ? WHERE user_id = ? AND text = ?"
    const val INSERT_USER_ANSWERS =
        "INSERT OR IGNORE INTO user_answers (user_id, word_id) VALUES (?, ?)"

    const val INSERT_WORD = "INSERT OR IGNORE INTO words (user_id, text, translate) VALUES (?, ?, ?)"
    const val INSERT_USER = "INSERT OR IGNORE INTO users (chat_id, user_name) VALUES (?, ?)"
    const val UPDATE_FILE_ID = "UPDATE words SET file_id = ? WHERE user_id = ? AND text = ?"
    const val CHECK_FILE_ID_EXISTENCE = """
        SELECT EXISTS (
        SELECT 1 FROM words
        WHERE user_id = ? 
        AND text = ? 
        AND file_id IS NOT NULL
        )
    """
    const val GET_DICTIONARY = """
        SELECT w.text AS text, w.translate AS translate
        FROM words w
        WHERE user_id = ?
    """
    const val GET_PHOTO_FILE_ID = "SELECT file_id AS fileId FROM words WHERE user_id = ? AND text = ?"
    const val GET_PERSONAL_WORDS_COUNT = "SELECT COUNT(*) AS count FROM user_answers WHERE user_id = ?"
    const val GET_NUM_OF_UNLEARNED_WORDS =
        "SELECT COUNT(*) AS count FROM user_answers WHERE user_id = ? AND correct_answer_count < ?"
    const val GET_NUM_OF_LEARNED_WORDS =
        "SELECT COUNT(*) AS count FROM user_answers WHERE user_id = ? AND correct_answer_count >= ?"
    const val GET_CURRENT_ANSWER_COUNT = """
            SELECT correct_answer_count AS count
            FROM user_answers
            WHERE user_id = ? AND word_id = ?
        """
    const val GET_LEARNED_WORDS = """
        SELECT w.text AS text, w.translate AS translate
        FROM words w
        JOIN user_answers ua 
        ON w.user_id = ua.user_id
        AND w.id = ua.word_id
        WHERE w.user_id = ? AND ua.correct_answer_count >= ? 
    """

    const val GET_UNLEARNED_WORDS = """
            SELECT w.text AS text, w.translate AS translate
            FROM words w
            JOIN user_answers ua 
            ON w.user_id = ua.user_id
            AND w.id = ua.word_id
            WHERE w.user_id = ? AND ua.correct_answer_count < ?
        """

    const val SET_CORRECT_ANSWERS_COUNT =
        "UPDATE user_answers SET correct_answer_count = ? WHERE user_id = ? AND word_id = ?"
    const val RESET_USER_PROGRESS =
        "UPDATE user_answers SET correct_answer_count = 0 WHERE user_id = ?"

    const val DELETE_WORDS = """
        DELETE FROM words
        WHERE user_id = ? 
    """

    const val DELETE_WORD = """
        DELETE FROM words
        WHERE user_id = ?
        AND text = ?
    """
    const val CHECK_WORD_EXISTENCE = "SELECT 1 FROM words WHERE text = ? AND user_id = ?"
    const val GET_USER_ID = "SELECT id FROM users WHERE chat_id = ?"
    const val GET_WORD_ID = "SELECT id FROM words WHERE text = ? AND user_id = ?"
} 