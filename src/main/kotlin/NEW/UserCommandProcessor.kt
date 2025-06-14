package NEW

import bot.serializableClasses.BotUpdate

interface UserCommandProcessor {
    fun run()
    fun handleUpdate(update: BotUpdate)
}