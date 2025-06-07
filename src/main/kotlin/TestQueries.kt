object TestQueries {

    const val INSERT_USER = "INSERT OR REPLACE INTO users (chat_id, user_name) VALUES (?, ?)"
    const val INSERT_WORD = "INSERT OR REPLACE INTO words (text, translate) VALUES (?, ?)"
}