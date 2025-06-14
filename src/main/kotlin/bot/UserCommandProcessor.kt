package bot

import serializableClasses.BotUpdate

interface UserCommandProcessor {
    fun run()
    fun handleUpdate(update: BotUpdate)
}