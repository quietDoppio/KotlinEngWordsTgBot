class MessagesIdsContainer {
    private val chatMessagesIds: MutableMap<Long, MutableList<Long>> = mutableMapOf()

    fun addId(chatId: Long, messageId: Long) {
        chatMessagesIds.getOrPut(chatId) { mutableListOf() }
            .run { if (messageId != 0L) add(messageId) else println("Не добавлено. messageId - 0") }
    }

    fun addId(chatId: Long, messagesIds: List<Long>) {
        chatMessagesIds.getOrPut(chatId) { mutableListOf() }
            .run { addAll(messagesIds.filter { it != 0L }) }
    }

    fun getIds(chatId: Long): List<Long> = chatMessagesIds[chatId] ?: emptyList()

    fun getLastId(chatId: Long): Long = chatMessagesIds[chatId]?.lastOrNull() ?: 0L

    fun clearIds(chatId: Long) {
        chatMessagesIds[chatId]?.clear()
    }
}