package bot

import serializableClasses.BotUpdate
import serializableClasses.Document
import config.BotConfig.App.UNKNOWN_USER
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import serializableClasses.PhotoSize

class BotUpdateProcessor(
    private val dataStorage: UserBotDataStorage,
    private val sessionHandler: SessionHandler,
    private val botController: BotController,
    private val getUpdates: (Long) -> List<BotUpdate>,
    private val addNewUserIfNotExists: (Long, String) -> Unit,
) {
    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        println("Caught $exception\n")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    private var updates: List<BotUpdate> = emptyList()

    private var lastUpdateId: Long = 0L

    suspend fun run() {
        while (true) {
            updates = withContext(Dispatchers.IO) { getUpdates(lastUpdateId) }
            if (updates.isNotEmpty()) {
                lastUpdateId = updates.last().updateId + 1
                distributeAndRun(updates)
            }
            delay(1000)
        }
    }

    private fun distributeAndRun(updates: List<BotUpdate>) {
        val grouped = updates.groupBy { it.fetchChatId() }

        for ((chatId, updates) in grouped) {
            chatId ?: continue

            println("id: $chatId - updates: ${updates.size}")
            scope.launch {
                runHandling(updates)
            }
        }
    }

    fun runHandling(updates: List<BotUpdate>) {
        updates.forEach {
            handleUpdate(it)
        }
    }

private fun handleUpdate(update: BotUpdate) {
    val data = extractUpdateData(update) ?: return
    val chatId = data.chatId
    val session = dataStorage.getSession(chatId)

    addNewUserIfNotExists(chatId, data.username)

    sessionHandler.startSession(session, data)
    botController.handleCallbackData(data.callbackData, chatId)
    botController.handleAddWordsFromFile(chatId, data.document)
    botController.sendMessageByKeyWord(data.message, chatId)
}

private fun BotUpdate.fetchChatId(): Long? =
    message?.chat?.chatId ?: callbackQuery?.message?.chat?.chatId

private fun extractUpdateData(update: BotUpdate): UpdateData? {
    val chatId = update.fetchChatId() ?: return null

    return UpdateData(
        chatId = chatId,
        message = update.message?.text ?: "",
        document = update.message?.document,
        photo = update.message?.photo?.get(1),
        callbackData = update.callbackQuery?.data ?: "",
        username = update.message?.from?.username ?: update.callbackQuery?.from?.username ?: UNKNOWN_USER,
        messageId = update.message?.messageId ?: 0L,
    )
}

data class UpdateData(
    val chatId: Long,
    val message: String,
    val document: Document?,
    val photo: PhotoSize?,
    val callbackData: String,
    val username: String,
    val messageId: Long,
)
}
