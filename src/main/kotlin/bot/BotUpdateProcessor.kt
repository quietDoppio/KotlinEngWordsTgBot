package bot

import MessagesIdsContainer
import SessionStorage
import Sessions
import Sessions.AddPictureSession
import Sessions.AddWordsSession
import Sessions.EditWordSession
import Sessions.SelectWordSession
import bot.utils.WaitingFor
import utils.FilesHelper
import utils.TelegramMessenger
import utils.UpdateSource
import serializableClasses.MessageResponse
import serializableClasses.BotUpdate
import kotlinx.serialization.json.Json
import serializableClasses.Document
import java.io.File
import config.BotConfig
import config.BotConfig.Learning.POLLING_INTERVAL_MS
import config.BotConfig.Telegram.Callbacks
import config.BotConfig.Telegram.Callbacks.ADD_WORDS_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.ANSWER_PREFIX
import config.BotConfig.Telegram.Callbacks.DELETE_DICTIONARY_CLICKED
import config.BotConfig.Telegram.Callbacks.DELETE_WORD_CLICKED
import config.BotConfig.Telegram.Callbacks.EDIT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.GO_BACK_SELECT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.GO_BACK_WORD_MENU_CLICKED
import config.BotConfig.Telegram.Callbacks.RESET_CLICKED
import config.BotConfig.Telegram.Callbacks.SELECT_WORD_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.SET_PICTURE_SESSION_CLICKED
import config.BotConfig.Telegram.Callbacks.START_LEARNING_CLICKED
import config.BotConfig.Telegram.Callbacks.STATISTICS_CLICKED
import config.BotConfig.Telegram.Emojis.CHECKMARK
import config.BotConfig.Telegram.Emojis.CROSSMARK
import config.BotConfig.Telegram.Emojis.ENG_FLAG
import config.BotConfig.Telegram.Emojis.HUNDRED
import config.BotConfig.Telegram.Emojis.RU_FLAG
import serializableClasses.PhotoSize
import utils.TextChecker

class BotUpdateProcessor(
    private val updatesSource: UpdateSource,
    private val messenger: TelegramMessenger,
    private val filesHelper: FilesHelper,
    private val trainer: LearnWordsTrainer,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var sessions: SessionStorage = SessionStorage()
    private val messagesIdsContainer: MessagesIdsContainer = MessagesIdsContainer()
    private var lastUpdateId: Long = 0

    private var currentEditedWord: Pair<Word, String>? = null
    private var currentWords: List<Word> = emptyList()

    val textChecker = TextChecker(
        messagesIdsContainer,
        messenger
    ) { getMessageIdFromResponse(it) }

    fun run() {
        println("Бот начал работу")
        while (true) {
            try {
                Thread.sleep(POLLING_INTERVAL_MS)

                val updates = updatesSource.getUpdates(lastUpdateId)
                if (updates.isNotEmpty()) {
                    lastUpdateId = updates.last().updateId + 1
                    updates.forEach { handleUpdate(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("restart")
                continue
            }
        }
    }

    private fun handleUpdate(update: BotUpdate) {
        //println(update)
        val updateData = extractUpdateData(update) ?: return

        val chatId = updateData.chatId
        val document = updateData.document
        val message = updateData.message ?: ""
        val callbackData = updateData.callbackData

        val session = sessions.getSession(chatId)

        //Регистрируем пользователя
        trainer.userRepository.addNewUser(chatId, updateData.username)

        //Старт обработки входящих сообщений/документов
        startSession(
            session = session,
            message = message,
            data = callbackData,
            document = document,
            photo = updateData.photo,
            chatId = chatId
        )

        handleCallbackData(callbackData, chatId)
        if (document != null && session == null && currentWords.isEmpty()) addWordsFromFile(chatId, document)
        sendMessageByKeyWord(message, chatId)
    }

    fun clearAll(chatId: Long) {
        deletePreviousMessages(chatId)
        sessions.clearSession(chatId)
        currentWords = emptyList()
        currentEditedWord = null
    }

    fun sendMessageByKeyWord(keyWord: String, chatId: Long) {
        if (keyWord == "/start") {
            clearAll(chatId)
            val menuResponse = messenger.sendMainMenu(chatId)
            messagesIdsContainer.addId(chatId, getMessageIdFromResponse(menuResponse))
        }
    }


    private fun extractUpdateData(update: BotUpdate): UpdateData? {
        val chatId = update.message?.chat?.chatId ?: update.callbackQuery?.message?.chat?.chatId ?: return null

        val message = update.message?.text
        val document = update.message?.document
        val photo = update.message?.photo?.get(1)
        val callbackData = update.callbackQuery?.data ?: ""
        val username =
            update.message?.from?.username ?: update.callbackQuery?.from?.username ?: BotConfig.App.UNKNOWN_USER
        val messageId = update.message?.messageId ?: 0L

        return UpdateData(
            chatId = chatId,
            message = message,
            document = document,
            photo = photo,
            callbackData = callbackData,
            username = username,
            messageId = messageId,
        )
    }

    private data class UpdateData(
        val chatId: Long,
        val message: String?,
        val document: Document?,
        val photo: PhotoSize?,
        val callbackData: String,
        val username: String,
        val messageId: Long,
    )

    private fun startSession(
        session: Sessions?,
        message: String,
        data: String,
        document: Document?,
        photo: PhotoSize?,
        chatId: Long
    ) {
        when (session) {
            is AddWordsSession -> {
                val waitingFor = session.waitingFor

                if (
                    message.isBlank() ||
                    !textChecker.isTextAlphabetCorrect(chatId, waitingFor, message) ||
                    !textChecker.isLineCorrect(chatId, message)
                ) return

                when (waitingFor) {
                    WaitingFor.ORIGINAL -> {
                        deletePreviousMessages(chatId, false, offset = 1)

                        session.currentOriginal = message
                        session.waitingFor = WaitingFor.TRANSLATION

                        val response: String = messenger.editWordRequest(
                            chatId,
                            messagesIdsContainer.getLastId(chatId),
                            "Введите перевод $RU_FLAG:"
                        )

                        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
                    }

                    WaitingFor.TRANSLATION -> {
                        deletePreviousMessages(chatId)

                        val original = session.currentOriginal.lowercase()
                        val translation = message.lowercase()
                        val newWord =
                            Word(originalWord = original, translatedWord = translation)

                        session.waitingFor = WaitingFor.ORIGINAL

                        filesHelper.saveWordsToRepository(chatId, newWord)
                        trainer.userRepository.addUserAnswersToUser(chatId, listOf(newWord))

                        val wordAddedResponse =
                            messenger.sendMessage(chatId, "Добавлено! ${CHECKMARK}\n$original - $translation")

                        val wordRequestResponse: String = messenger.sendWordsRequest(
                            chatId, "Введите оригинал ${ENG_FLAG}:"
                        )

                        messagesIdsContainer.addId(
                            chatId,
                            listOf(
                                getMessageIdFromResponse(wordAddedResponse),
                                getMessageIdFromResponse(wordRequestResponse)
                            )
                        )
                    }
                }
            }

            is SelectWordSession -> {
                val userNumInput = message.toIntOrNull()
                val wordsAmountRange = 1..currentWords.size

                if (userNumInput == null) {
                    return
                } else if (userNumInput !in wordsAmountRange) {
                    val sendMessageResponse = messenger.sendMessage(chatId, "Введите номер слова из списка")
                    messagesIdsContainer.addId(chatId, getMessageIdFromResponse(sendMessageResponse))
                    return
                }

                val indexedWords = currentWords.withIndex().associate { (it.index + 1) to it.value }
                val selectedWord = indexedWords[userNumInput] ?: return

                deletePreviousMessages(chatId)

                val fileId = trainer.wordRepository.getFileId(chatId, selectedWord.originalWord) ?: ""

                currentEditedWord = selectedWord to fileId
                sessions.clearSession(chatId)

                showEditWordMenu(chatId, fileId, selectedWord)
            }

            is EditWordSession -> {
                val waitingFor = session.waitingFor

                if (
                    currentEditedWord == null ||
                    data.isNotEmpty() ||
                    !textChecker.isTextAlphabetCorrect(chatId, waitingFor, message) ||
                    !textChecker.isLineCorrect(chatId, message)
                ) return

                var word = currentEditedWord!!.first
                val fileId = currentEditedWord!!.second

                when (waitingFor) {
                    WaitingFor.ORIGINAL -> {
                        val engInputWord = message.lowercase()
                        val translateResponse = messenger.editWordRequest(
                            chatId,
                            messagesIdsContainer.getLastId(chatId),
                            "Введите перевод ${RU_FLAG}\n" +
                                    "$engInputWord >> ${word.translatedWord} <<",
                            false
                        )
                        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(translateResponse))
                        session.currentOriginal = engInputWord
                        session.waitingFor = WaitingFor.TRANSLATION
                    }

                    WaitingFor.TRANSLATION -> {
                        val ruInputWord = message.lowercase()
                        val newWord =
                            Word(
                                originalWord = session.currentOriginal.lowercase(),
                                translatedWord = ruInputWord
                            )
                        currentEditedWord = newWord to fileId

                        filesHelper.editWordInRepository(chatId, word, newWord)

                        val doneResponse = messenger.sendMessage(chatId, "Изменено $CHECKMARK")
                        messagesIdsContainer.addId(
                            chatId, listOf(getMessageIdFromResponse(doneResponse))
                        )

                        sessions.clearSession(chatId)
                        deletePreviousMessages(chatId, false, offset = 1)
                        showEditWordMenu(chatId, fileId, newWord)
                    }
                }
            }

            is AddPictureSession -> {
                val word = currentEditedWord?.first

                if (photo == null) {
                    if (data.isNotEmpty()) {
                        return
                    } else if (document?.fileName?.endsWith(".jpg") == true) {
                        val response = messenger.sendMessage(chatId, "Необходимо сжатое изображение")
                        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
                        return
                    }
                    val response = messenger.sendMessage(chatId, "Прикрепите изображение")
                    messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
                    return
                }

                trainer.wordRepository.updateFileId(chatId, photo.fileId, word?.originalWord ?: "")

                val response = messenger.sendMessage(chatId, "Прикреплено! $CHECKMARK")
                messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))

                sessions.clearSession(chatId)
                showEditWordMenu(chatId, photo.fileId, word)
            }

            null -> return
        }
    }

    private fun addWordsFromFile(chatId: Long, document: Document) {
        val addWordsStatus = filesHelper.addWordsFromFile(document, chatId)
        if (addWordsStatus) {
            deletePreviousMessages(chatId)
            val response = messenger.sendMessage(chatId, "Слова добавлены!")
            messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
            handleSelectWordSession(chatId, 1)
        }
    }

    private fun handleAddWords(chatId: Long) {
        val response =
            messenger.sendWordsRequest(chatId, "Введите оригинал ${ENG_FLAG}:")
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))

        sessions.setSession(chatId, AddWordsSession())
    }

    private fun getMessageIdFromResponse(response: String): Long =
        runCatching { json.decodeFromString<MessageResponse>(response).result?.messageId ?: 0L }.getOrNull() ?: 0L


    private fun runWithMessagesCleanup(chatId: Long, data: String, resultHandler: () -> Unit) {
        deletePreviousMessages(chatId)
        resultHandler()
    }

    fun handleCallbackData(data: String, chatId: Long) {
        when {
            data.isEmpty() -> return
            data.startsWith(ANSWER_PREFIX) -> {
                handleAnswerSubmission(data, chatId); return
            }

            RESET_CLICKED == data -> {
                handleResetStatistics(chatId); return
            }

            STATISTICS_CLICKED == data -> {
                handleShowStatistics(chatId); return
            }
        }

        runWithMessagesCleanup(chatId, data) {
            when (data) {
                GO_BACK_SELECT_WORD_SESSION_CLICKED -> handleSelectWordSession(chatId)
                SELECT_WORD_SESSION_CLICKED -> handleSelectWordSession(chatId)
                SET_PICTURE_SESSION_CLICKED -> handleSetPictureSession(chatId)
                DELETE_WORD_CLICKED -> handleDeleteWord(chatId)
                EDIT_WORD_SESSION_CLICKED -> handleEditWordSession(chatId)
                START_LEARNING_CLICKED -> handleStartLearning(chatId)
                ADD_WORDS_SESSION_CLICKED -> handleAddWords(chatId)
                DELETE_DICTIONARY_CLICKED -> handleDeleteAllWords(chatId)
                GO_BACK_WORD_MENU_CLICKED -> {
                    sessions.clearSession(chatId)
                    showEditWordMenu(chatId, currentEditedWord?.second ?: "", currentEditedWord?.first)
                }

                Callbacks.RETURN_MAIN_MENU_CLICKED -> {
                    clearAll(chatId)
                    showMainMenu(chatId)
                }
            }
        }
    }

    fun showEditWordMenu(chatId: Long, fileId: String = "", selectedWord: Word?) {
        selectedWord?.let { word ->
            val sendPictureResponse: String =
                messenger.sendPhotoByFileId(fileId, chatId, false)

            val editWordMenuResponse: String =
                messenger.sendWordMenu(
                    chatId, "${word.originalWord} - ${word.translatedWord}"
                )

            messagesIdsContainer.addId(
                chatId,
                listOf(
                    getMessageIdFromResponse(sendPictureResponse),
                    getMessageIdFromResponse(editWordMenuResponse)
                )
            )
        }

    }

    private fun handleSetPictureSession(chatId: Long) {
        sessions.setSession(chatId, AddPictureSession)
        showSetPictureMenu(chatId)
    }

    private fun showSetPictureMenu(chatId: Long) {
        val response = messenger.sendAddPictureMenu(chatId, "Прикрепите изображение")
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
    }

    private fun handleDeleteWord(chatId: Long) {
        val word: String = currentEditedWord?.first?.originalWord ?: ""
        trainer.userRepository.deleteWord(chatId, word)

        val stringToSend = "Слово \"${word}\" удалено!"
        val response = messenger.sendMessage(chatId, stringToSend)
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))

        currentEditedWord = null
        handleSelectWordSession(chatId, 1)
    }

    private fun handleEditWordSession(chatId: Long) {
        val word = currentEditedWord?.first ?: return

        sessions.setSession(chatId, EditWordSession())

        val response = messenger.sendWordsRequest(
            chatId,
            "Введите оригинал ${ENG_FLAG}\n>> ${word.originalWord} << ${word.translatedWord}",
            false
        )
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
    }

    private fun handleSelectWordSession(chatId: Long, prevMessageOffset: Int = 0) {
        currentEditedWord = null
        currentWords = trainer.userRepository.getDictionary(chatId)

        if (currentWords.isEmpty()) {
            val showWordsMenuResponse = messenger.sendShowWordsMenu(chatId, "Словарь пуст", true)
            messagesIdsContainer.addId(chatId, getMessageIdFromResponse(showWordsMenuResponse))
            return
        }

        deletePreviousMessages(chatId, clearIds = false, offset = prevMessageOffset)

        val dictionaryString = currentWords.mapIndexed { index, word ->
            "${index + 1}. ${word.originalWord} - ${word.translatedWord}"
        }.joinToString("\n")

        val response =
            messenger.sendShowWordsMenu(chatId, "Напишите номер слова для редактирования:\n\n$dictionaryString")
        messagesIdsContainer.addId(chatId, listOf(getMessageIdFromResponse(response)))

        sessions.setSession(chatId, SelectWordSession)
    }

    private fun deletePreviousMessages(chatId: Long, clearIds: Boolean = true, offset: Int = 0) {
        val caller = Throwable().stackTrace[2]
        println("deletePreviousMessages вызвана из: ${caller.className}.${caller.methodName} (${caller.fileName}:${caller.lineNumber})")

        messenger.deleteMessages(chatId, messagesIdsContainer.getIds(chatId).dropLast(offset))
        if (clearIds) messagesIdsContainer.clearIds(chatId)
    }

    private fun handleStartLearning(chatId: Long) {
        checkNextQuestionAndSend(chatId)
    }

    private fun handleDeleteAllWords(chatId: Long) {
        trainer.deleteAllWords(chatId)
        clearAll(chatId)

        val messageResponse: String = messenger.sendMessage(chatId, "Все слова удалены")
        val showWordsResponse: String = messenger.sendShowWordsMenu(chatId, "Словарь пуст", true)
        messagesIdsContainer.addId(
            chatId,
            listOf(
                getMessageIdFromResponse(messageResponse),
                getMessageIdFromResponse(showWordsResponse)
            )
        )
    }


    private fun showMainMenu(chatId: Long) {
        val mainMenuResponse = messenger.sendMainMenu(chatId)
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(mainMenuResponse))
    }

    private fun handleResetStatistics(chatId: Long) {
        trainer.resetStatistics(chatId)
        val statistics = trainer.getStatistics(chatId)
        val messageResponse = messenger.sendMessage(
            chatId, "Статистика успешно сброшена\n${
                BotConfig.Messages.STATISTIC_FORMAT.format(
                    statistics.learnedWordsCount,
                    statistics.totalWordsCount,
                    statistics.learnedWordsPercent
                )
            } "
        )
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(messageResponse))
    }

    private fun handleShowStatistics(chatId: Long) {
        val statistics = trainer.getStatistics(chatId)
        val response = messenger.sendMessage(
            chatId,
            BotConfig.Messages.STATISTIC_FORMAT.format(
                statistics.learnedWordsCount,
                statistics.totalWordsCount,
                statistics.learnedWordsPercent
            )
        )
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
    }

    private fun handleAnswerSubmission(data: String, chatId: Long) {
        val indexOfClicked = data.substringAfter(ANSWER_PREFIX).toInt()
        val isRightAnswer = trainer.checkAnswer(indexOfClicked, chatId)

        val correctAnswer = trainer.question?.correctAnswer
        val wordsMessage = "${correctAnswer?.originalWord} - ${correctAnswer?.translatedWord}"

        val response = if (isRightAnswer) {
            messenger.sendMessage(chatId, "$CHECKMARK Верно! $wordsMessage")
        } else {
            val message = "$CROSSMARK Не верно! $wordsMessage"
            messenger.sendMessage(chatId, message)
        }
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))

        deletePreviousMessages(chatId, false, 1)
        checkNextQuestionAndSend(chatId)
    }

    private fun checkNextQuestionAndSend(chatId: Long) {
        val question = trainer.getNextQuestion(chatId)
        if (question == null) {
            sendCompletionMessage(chatId)
        } else {
            sendQuestion(chatId, question)
        }
    }

    private fun sendCompletionMessage(chatId: Long) {
        deletePreviousMessages(chatId)

        val messageResponse = messenger.sendMessage(
            chatId, "$HUNDRED Вы выучили все слова в базе или они отсутствуют"
        )

        val sendMenuResponse = messenger.sendMainMenu(chatId)
        messagesIdsContainer.addId(
            chatId,
            listOf(
                getMessageIdFromResponse(messageResponse),
                getMessageIdFromResponse(sendMenuResponse)
            )
        )
    }

    private fun sendQuestion(chatId: Long, question: Question) {
        val originalWord = question.correctAnswer.originalWord

        sendImageForWord(chatId, originalWord)

        val sendQuestionResponse = messenger.sendQuestion(chatId, question)
        messagesIdsContainer.addId(chatId, getMessageIdFromResponse(sendQuestionResponse))
    }

    private fun sendImageForWord(chatId: Long, originalWord: String) {
        if (trainer.wordRepository.checkFileIdExistence(chatId, originalWord)) sendImageByFileId(chatId, originalWord)
    }

    private fun sendImageByFileId(chatId: Long, originalWord: String) {
        trainer.wordRepository.getFileId(chatId, originalWord)?.let { fileId ->

            val response = messenger.sendPhotoByFileId(fileId = fileId, chatId = chatId, hasSpoiler = true)
            messagesIdsContainer.addId(chatId, getMessageIdFromResponse(response))
        }
    }
}
