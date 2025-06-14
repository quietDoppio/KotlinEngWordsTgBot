package NEW

import bot.serializableClasses.BotUpdate

interface UpdateSource {
    fun getUpdates(lastUpdateId: Long): List<BotUpdate>
}