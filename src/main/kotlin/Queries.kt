object Queries {
    const val CREATE_TABLE_WORDS = """
        CREATE TABLE IF NOT EXISTS words (
        id INTEGER PRIMARY KEY,      
        text VARCHAR UNIQUE,
        translate VARCHAR,
        file_id VARCHAR UNIQUE DEFAULT NULL
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
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
        FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE
        )
    """
    const val INSERT_USER_ANSWERS = """
            INSERT OR IGNORE INTO user_answers (user_id, word_id)
            VALUES (
            (SELECT id FROM users WHERE chat_id = ?), 
            (SELECT id FROM words WHERE text = ?)
            )
        """
    const val INSERT_WORD = "INSERT OR IGNORE INTO words (text, translate) VALUES (?, ?)"
    const val INSERT_USER = "INSERT OR IGNORE INTO users (chat_id, user_name) VALUES (?, ?)"
    const val UPDATE_FILE_ID = "UPDATE words SET file_id = ? WHERE text = ?"
    const val CHECK_FILE_ID_EXISTENCE = "SELECT EXISTS (SELECT 1 FROM words WHERE text = ? AND file_id IS NOT NULL)"
    const val GET_FILE_ID = "SELECT file_id FROM words WHERE text = ?"
    const val DELETE_FROM_WORDS = "DELETE FROM words"
    const val DELETE_FROM_USERS = "DELETE FROM users"
    const val DELETE_FROM_USER_ANSWERS = "DELETE FROM user_answers"
    const val GET_PERSONAL_WORDS_COUNT =
        "SELECT COUNT(*) FROM user_answers WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)"
    const val GET_NUM_OF_UNLEARNED_WORDS = """
            SELECT COUNT(*)
            FROM user_answers ua
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?) 
            AND ua.correct_answer_count < ?
        """
    const val GET_NUM_OF_LEARNED_WORDS = """ 
            SELECT COUNT(*) 
            FROM user_answers ua
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
            AND ua.correct_answer_count >= ?
        """
    const val GET_CURRENT_ANSWER_COUNT = """
            SELECT correct_answer_count
            FROM user_answers
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
            AND word_id = (SELECT id FROM words WHERE text = ?)
        """
}