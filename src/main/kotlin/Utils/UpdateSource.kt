package Utils

import api.TelegramApiService
import kotlinx.serialization.json.Json
import serializableClasses.BotUpdate
import serializableClasses.Response

interface UpdateSource {
    fun getUpdates(lastUpdateId: Long): List<BotUpdate>
}

class TelegramUpdateSourceImpl(
    private val api: TelegramApiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : UpdateSource {

    override fun getUpdates(lastUpdateId: Long): List<BotUpdate> {
        val responseString = api.getUpdates(lastUpdateId)
        val responseObj = json.decodeFromString<Response>(responseString)
        return responseObj.updates
    }
}