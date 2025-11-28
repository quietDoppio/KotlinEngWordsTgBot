package bot

import api.TelegramApiService
import bot.SessionStorage.Sessions.AddPictureSession
import bot.SessionStorage.Sessions.AddWordsSession
import bot.SessionStorage.Sessions.EditWordSession
import bot.SessionStorage.Sessions.LearnWordsSession
import bot.SessionStorage.Sessions.SelectWordSession
import config.BotConfig.Files.pictureEnds
import config.TelegramConfig.Messages.COMPRESSED_PICTURE_REQUIRED
import config.TelegramConfig.Messages.ENTER_CYRILLIC
import config.TelegramConfig.Messages.ENTER_LATIN
import config.TelegramConfig.Messages.ENTER_NUM_OF_WORDS_LIST
import config.TelegramConfig.Messages.INCORRECT_INPUT
import config.TelegramConfig.Messages.ORIGINAL_WORD_REQUEST
import config.TelegramConfig.Messages.PICTURE_HAS_BEEN_SET
import config.TelegramConfig.Messages.SET_PICTURE_REQUEST
import config.TelegramConfig.Messages.TRANSLATE_REQUEST
import config.TelegramConfig.Messages.WORD_ADDED
import config.TelegramConfig.Messages.WORD_HAS_CHANGED
import config.TelegramConfig.Messages.WORD_HAS_NOT_ADDED
import config.TelegramConfig.Messages.WORD_HAS_NOT_CHANGED
import database.DataBaseRepository
import serializableClasses.Document
import serializableClasses.PhotoSize
import utils.TelegramMessenger
import utils.TextChecker.isInputCorrect
import utils.TextChecker.isTextCyrillic
import utils.TextChecker.isTextLatin

class SessionHandler(
    private val service: TelegramApiService,
    private val repository: DataBaseRepository,
    private val dataStorage: UserBotDataStorage,
    private val messenger: TelegramMessenger,
    private val showEditWordMenu: (Long, Word) -> Unit,
) {
    fun startSession(session: SessionStorage.Sessions?, data: BotUpdateProcessor.UpdateData) {
        val chatId = data.chatId
        val currentWords = dataStorage.getCurrentWords(chatId).ifEmpty { null }
        val currentWord = dataStorage.getCurrentWord(chatId)

        if (data.callbackData.isNotEmpty()) return
        when (session) {
            is AddWordsSession -> handleAddWordsSession(chatId, session, data.message)
            is SelectWordSession -> currentWords?.let { words ->
                startSelectWordSession(chatId, data.message, words)
            }

            is EditWordSession -> currentWord?.let { word ->
                handleEditWordSession(chatId, session, data.message, word)
            }

            is AddPictureSession -> currentWord?.let { word ->
                handleAddPictureSession(chatId, data.photo, data.document, data.callbackData, word)
            }

            is LearnWordsSession -> return
            null -> return
        }
    }

    private fun handleAddWordsSession(chatId: Long, session: AddWordsSession, message: String) {
        if (message.isEmpty() || !message.isInputCorrect()) {
            messenger.sendSimpleTextAndSave(chatId, INCORRECT_INPUT)
             return
        }

        when (session.waitingFor) {
            SessionStorage.WaitingFor.ORIGINAL -> {
                if (!message.isTextLatin()) {
                    messenger.sendSimpleTextAndSave(chatId, ENTER_LATIN)
                    return
                }
                val isWordExist = repository.checkWordExistence(chatId, message)
                if(isWordExist) {
                    messenger.sendSimpleTextAndSave(chatId, WORD_HAS_NOT_ADDED.format(message))
                    return
                }

                session.currentOriginal = message
                session.waitingFor = SessionStorage.WaitingFor.TRANSLATION
                val lastMessageId = dataStorage.getLastMessageId(chatId)

                messenger.deletePrevious(chatId = chatId, false, offset = 1)
                messenger.sendAndSave(chatId) {
                    service.editWordRequest(chatId, lastMessageId, TRANSLATE_REQUEST, false)
                }

            }

            SessionStorage.WaitingFor.TRANSLATION -> {
                if (!message.isTextCyrillic()) {
                    messenger.sendSimpleTextAndSave(chatId, ENTER_CYRILLIC)
                    return
                }

                val original = session.currentOriginal.lowercase()
                val translation = message.lowercase()
                val newWord = Word(originalWord = original, translatedWord = translation)

                session.waitingFor = SessionStorage.WaitingFor.ORIGINAL

                repository.addWords(chatId, newWord)
                repository.addUserAnswersToUser(chatId, listOf(newWord))

                messenger.deletePrevious(chatId)
                messenger.sendSimpleTextAndSave(chatId, "${WORD_ADDED}\n$original - $translation")
                messenger.sendAndSave(chatId) { service.sendWordsRequest(chatId, ORIGINAL_WORD_REQUEST) }
            }
        }
    }

    private fun handleEditWordSession(
        chatId: Long,
        session: EditWordSession,
        message: String,
        currentWord: Word?
    ) {
        currentWord ?: return
        if (message.isBlank() || !message.isInputCorrect()) {
            messenger.sendSimpleTextAndSave(chatId, INCORRECT_INPUT)
            return
        }

        when (session.waitingFor) {
            SessionStorage.WaitingFor.ORIGINAL -> {
                if (!message.isTextLatin()) {
                    messenger.sendSimpleTextAndSave(chatId, ENTER_LATIN)
                    return
                }
                val isWordExist = repository.checkWordExistence(chatId, message)
                if(isWordExist) {
                    messenger.sendSimpleTextAndSave(chatId, WORD_HAS_NOT_CHANGED.format(message))
                    return
                }

                val engInputWord = message.lowercase()
                val messageToSend = "${TRANSLATE_REQUEST}\n$engInputWord >> ${currentWord.translatedWord.uppercase()} <<"
                val lastMessageId = dataStorage.getLastMessageId(chatId)

                session.currentOriginal = engInputWord
                session.waitingFor = SessionStorage.WaitingFor.TRANSLATION

                messenger.sendAndSave(chatId) {
                    service.editWordRequest(chatId, lastMessageId, messageToSend, false)
                }
            }

            SessionStorage.WaitingFor.TRANSLATION -> {
                if (!message.isTextCyrillic()) {
                    messenger.sendSimpleTextAndSave(chatId, ENTER_CYRILLIC)
                    return
                }

                val ruInputWord = message.lowercase()
                val newWord = Word(session.currentOriginal.lowercase(), ruInputWord)

                repository.editWord(chatId, currentWord, newWord)
                dataStorage.setCurrentWord(chatId, newWord)

                dataStorage.clearSession(chatId)

                messenger.deletePrevious(chatId)
                messenger.sendSimpleTextAndSave(chatId, WORD_HAS_CHANGED)
                showEditWordMenu(chatId, newWord)
            }
        }
    }

    private fun startSelectWordSession(chatId: Long, message: String, currentWords: List<Word>) {
        val userNumInput = message.toIntOrNull()
        val wordsAmountRange = 1..currentWords.size

        if (userNumInput == null || userNumInput !in wordsAmountRange) {
            messenger.sendSimpleTextAndSave(chatId, ENTER_NUM_OF_WORDS_LIST)
            return
        }

        val indexedWords: Map<Int, Word> = currentWords.withIndex().associate { (it.index + 1) to it.value }
        val selectedWord = indexedWords[userNumInput] ?: return

        messenger.deletePrevious(chatId)

        dataStorage.setCurrentWord(chatId, selectedWord)
        dataStorage.clearSession(chatId)

        showEditWordMenu(chatId, selectedWord)
    }

    private fun handleAddPictureSession(
        chatId: Long,
        photo: PhotoSize?,
        document: Document?,
        callbackData: String,
        currentWord: Word,
    ) {
        if (photo == null) {
            if (callbackData.isNotEmpty()) return
            if (document != null && pictureEnds.any { document.fileName.endsWith(it) }) {
                messenger.sendSimpleTextAndSave(chatId, COMPRESSED_PICTURE_REQUIRED)
                return
            }
            messenger.sendSimpleTextAndSave(chatId, SET_PICTURE_REQUEST)
            return
        }

        repository.updateFileId(chatId, photo.fileId, currentWord.originalWord)
        dataStorage.clearSession(chatId)

        messenger.sendSimpleTextAndSave(chatId, PICTURE_HAS_BEEN_SET)
        showEditWordMenu(chatId, currentWord)
    }
}