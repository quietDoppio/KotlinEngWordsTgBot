package utils

import MessagesIdsContainer
import bot.utils.WaitingFor

class TextChecker(
    val idsContainer: MessagesIdsContainer,
    val messenger: TelegramMessenger,
    val getMessageIdFromResponse: (String) -> Long
) {
    internal fun isTextAlphabetCorrect(chatId: Long, waitingFor: WaitingFor, text: String): Boolean {
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        val hasCyrillic = text.any { it in 'А'..'я' || it == 'Ё' || it == 'ё' }
        return if (waitingFor == WaitingFor.ORIGINAL) {
            if (hasLatin && !hasCyrillic) {
                true
            } else {
                val response = messenger.sendMessage(chatId, "Введите слово на латинице")
                idsContainer.addId(chatId, getMessageIdFromResponse(response))
                false
            }
        } else {
            if (!hasLatin && hasCyrillic) {
                true
            } else {
                val response = messenger.sendMessage(chatId, "Введите слово на кириллице")
                idsContainer.addId(chatId, getMessageIdFromResponse(response))
                false
            }
        }
    }

    internal fun isLineCorrect(chatId: Long, message: String): Boolean {
        val hasSymbols = message.any { it.isLetter() }
        val startsWithLetter = message.firstOrNull()?.isLetter() == true
        val noLineBreaks = !message.contains("\n")

        return if (hasSymbols && startsWithLetter && noLineBreaks) {
            true
        } else if (message.isBlank()) {
            false
        } else {
            val response = messenger.sendMessage(chatId, "Некорректная строка")
            idsContainer.addId(chatId, getMessageIdFromResponse(response))
            false
        }
    }
}