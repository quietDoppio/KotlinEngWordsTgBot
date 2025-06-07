package bot.utils

import bot.Word

data class AddWordsSessionState(
    var waitingFor: WaitingFor = WaitingFor.ORIGINAL,
    var currentOriginal: String = ""
)

enum class WaitingFor {
    ORIGINAL,    // Ожидание оригинального слова
    TRANSLATION  // Ожидание перевода
}

class AddWordsSessionHandler() {
    private val sessionsStorage = mutableMapOf<Long, AddWordsSessionState?>()

    fun getSession(chatId: Long): AddWordsSessionState? = sessionsStorage[chatId]


    fun setSession(chatId: Long, session: AddWordsSessionState?) {
        sessionsStorage[chatId] = session
    }

    fun clearSession(chatId: Long) {
        sessionsStorage.remove(chatId)
    }
}



