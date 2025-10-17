package utils

import api.TelegramApiService
import bot.IdsStorage
import config.TelegramConfig.CallbackData.CallbacksEnum
import kotlinx.serialization.json.Json
import serializableClasses.Message
import serializableClasses.TelegramResponse

class TelegramMessenger(
    private val service: TelegramApiService,
    private val idsStorage: IdsStorage,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun sendSimpleTextAndSave(chatId: Long, text: String) {
        val response = service.sendMessage(chatId, text)
        val messageId = extractMessageIdOrZero(response)
        idsStorage.addId(chatId = chatId, messageId)
    }

    fun sendAndSave(chatId: Long, messageBlock: () -> String) {
        val response = messageBlock()
        val messageId = extractMessageIdOrZero(response)
        idsStorage.addId(chatId = chatId, messageId)
    }

    fun deletePrevious(chatId: Long, clearIds: Boolean = true, offset: Int = 0) {
        val idsToDelete = idsStorage.getIds(chatId).safeDropLast(offset)
        if (idsToDelete.isEmpty()) return

        service.deleteMessages(chatId, idsToDelete)
        if (clearIds) idsStorage.clearIds(chatId)
    }

    fun runWithMessagesCleanup(
        chatId: Long,
        data: String,
        nonCleanupCallbacks: Set<CallbacksEnum> = emptySet(),
        resultHandler: (CallbacksEnum) -> Unit
    ) {
        val callback: CallbacksEnum? = CallbacksEnum.fromKey(data)
        if (callback == null) {
            println("Incorrect callback"); return
        }

        if (callback !in nonCleanupCallbacks) deletePrevious(chatId)

        resultHandler(callback)
    }

    private fun extractMessageIdOrZero(response: String): Long =
        runCatching {
            json.decodeFromString<TelegramResponse<Message>>(response).result?.messageId ?: 0L
        }.getOrNull() ?: 0L

    private fun List<Long>.safeDropLast(offset: Int): List<Long> =
        if (offset in 1 until size) dropLast(offset) else if (offset <= 0) this else emptyList()
}