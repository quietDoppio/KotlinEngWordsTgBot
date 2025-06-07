package utils

import api.TelegramApiService
import kotlinx.serialization.json.Json
import serializableClasses.BotUpdate
import serializableClasses.Response

class UpdateSource(
    private val api: TelegramApiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun getUpdates(lastUpdateId: Long): List<BotUpdate> {
        val responseString = api.getUpdates(lastUpdateId)
        val responseObj = json.decodeFromString<Response>(responseString)
        val updates = responseObj.updates.sortedBy { it.updateId }
        return updates
    }
}