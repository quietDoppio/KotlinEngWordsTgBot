import bot.utils.WaitingFor

class SessionStorage() {
    private val sessionsStorage = mutableMapOf<Long, Sessions?>()

    fun getSession(chatId: Long): Sessions? = sessionsStorage[chatId]


    fun setSession(chatId: Long, session: Sessions?) {
        sessionsStorage[chatId] = session
    }

    fun clearSession(chatId: Long) {
        sessionsStorage.remove(chatId)
    }

    fun getShowWordsSession(chatId: Long): Sessions.SelectWordSession? =
        runCatching { getSession(chatId) as Sessions.SelectWordSession }.getOrNull()

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

    object SelectWordSession: Sessions()
    object AddPictureSession : Sessions()
}