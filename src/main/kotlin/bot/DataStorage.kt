package bot

import bot.SessionStorage.Sessions

data class UserBotDataStorage(
    private val idsStorage: IdsStorage,
    private val sessionStorage: SessionStorage,
    private val wordsStorage: WordsStorage,
) {
    fun clearAll(chatId: Long) {
        idsStorage.clearIds(chatId)
        sessionStorage.clearSession(chatId)
        wordsStorage.clearAllWords(chatId)
    }

    fun getLastMessageId(chatId: Long): Long = idsStorage.getLastId(chatId)

    fun getSession(chatId: Long): Sessions? = sessionStorage.getSession(chatId)
    fun setSession(chatId: Long, session: Sessions?) {
        sessionStorage.setSession(chatId, session)
    }

    fun clearSession(chatId: Long) {
        sessionStorage.clearSession(chatId)
    }

    fun getCurrentWords(chatId: Long): List<Word> = wordsStorage.getCurrentWords(chatId)
    fun getCurrentWord(chatId: Long): Word? = wordsStorage.getCurrentWord(chatId)
    fun setCurrentWords(chatId: Long, words: List<Word>) {
        wordsStorage.setCurrentWords(chatId, words)
    }

    fun setCurrentWord(chatId: Long, word: Word?) {
        wordsStorage.setCurrentWord(chatId, word)
    }
}

class IdsStorage {
    private val chatMessagesIds: MutableMap<Long, MutableList<Long>> = mutableMapOf()

    fun addId(chatId: Long, vararg messagesIds: Long) {
        chatMessagesIds.getOrPut(chatId) { mutableListOf() }
            .run {
                val correctIds = messagesIds.filter { it != 0L }
                if (correctIds.isNotEmpty()) addAll(correctIds)
            }
    }

    fun getIds(chatId: Long): List<Long> = chatMessagesIds[chatId] ?: emptyList()

    fun getLastId(chatId: Long): Long = chatMessagesIds[chatId]?.lastOrNull() ?: 0L

    fun clearIds(chatId: Long) {
        chatMessagesIds.remove(chatId)
    }
}

class SessionStorage {
    private val sessionsStorage = mutableMapOf<Long, Sessions?>()

    fun getSession(chatId: Long): Sessions? = sessionsStorage[chatId]
    fun setSession(chatId: Long, session: Sessions?) {
        sessionsStorage[chatId] = session
    }

    fun clearSession(chatId: Long) {
        sessionsStorage.remove(chatId)
    }

    enum class WaitingFor {
        ORIGINAL,    // Ожидание оригинального слова
        TRANSLATION  // Ожидание перевода
    }

    sealed class Sessions {
        data class AddWordsSession(
            var waitingFor: WaitingFor = WaitingFor.ORIGINAL,
            var currentOriginal: String = "",
        ) : Sessions()

        data class EditWordSession(
            var waitingFor: WaitingFor = WaitingFor.ORIGINAL,
            var currentOriginal: String = ""
        ) : Sessions()

        object LearnWordsSession : Sessions()
        object SelectWordSession : Sessions()
        object AddPictureSession : Sessions()
    }
}

class WordsStorage {
    private val currentWords = mutableMapOf<Long, List<Word>>()
    private val currentWord = mutableMapOf<Long, Word?>()

    fun getCurrentWords(chatId: Long): List<Word> = currentWords.getOrPut(chatId) { mutableListOf() }
    fun getCurrentWord(chatId: Long): Word? = currentWord[chatId]

    fun setCurrentWords(chatId: Long, words: List<Word>) {
        currentWords[chatId] = words
    }

    fun setCurrentWord(chatId: Long, word: Word?) {
        currentWord[chatId] = word
    }

    fun clearAllWords(chatId: Long) {
        currentWords[chatId] = emptyList()
        currentWord.remove(chatId)
    }
}
