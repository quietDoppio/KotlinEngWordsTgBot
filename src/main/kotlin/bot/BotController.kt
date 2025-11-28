package bot

import api.TelegramApiService
import config.TelegramConfig
import config.TelegramConfig.Messages.ALL_WORDS_DELETED
import config.TelegramConfig.Messages.DICTIONARY_IS_EMPTY
import config.TelegramConfig.Messages.HINT_DELETED
import config.TelegramConfig.Messages.HINT_NOT_DELETED
import config.TelegramConfig.Messages.INFO_ADD_WORDS
import config.TelegramConfig.Messages.LEARNING_COMPLETED
import config.TelegramConfig.Messages.NUM_OF_WORD_REQUESTED
import config.TelegramConfig.Messages.ORIGINAL_WORD_REQUEST
import config.TelegramConfig.Messages.SET_PICTURE_REQUEST
import config.TelegramConfig.Messages.STATISTICS_RESET
import config.TelegramConfig.Messages.STATISTIC_FORMAT
import config.TelegramConfig.Messages.WORDS_ADDED
import config.TelegramConfig.Messages.WORDS_NOT_ADDED
import config.TelegramConfig.Messages.WORD_CORRECT_ANSWER
import config.TelegramConfig.Messages.WORD_DELETED
import config.TelegramConfig.Messages.WORD_INCORRECT_ANSWER
import database.DataBaseRepository
import serializableClasses.Document
import utils.FilesHelper
import utils.TelegramMessenger

class BotController(
    val repository: DataBaseRepository,
    val trainer: LearnWordsTrainer,
    val service: TelegramApiService,
    val dataStorage: UserBotDataStorage,
    val filesHelper: FilesHelper,
    val messenger: TelegramMessenger,
) {
    private val callbackActions: Map<TelegramConfig.CallbackData.CallbacksEnum, (Long) -> Unit> = mapOf(
        TelegramConfig.CallbackData.CallbacksEnum.GO_BACK_TO_SELECT_WORD_SESSION_CLICKED to { startSelectWordSession(it) },
        TelegramConfig.CallbackData.CallbacksEnum.SELECT_WORD_SESSION_CLICKED to { startSelectWordSession(it) },
        TelegramConfig.CallbackData.CallbacksEnum.SET_PICTURE_SESSION_CLICKED to { startSetPictureSession(it) },
        TelegramConfig.CallbackData.CallbacksEnum.ADD_WORDS_SESSION_CLICKED to { startAddWordsSession(it) },
        TelegramConfig.CallbackData.CallbacksEnum.EDIT_WORD_SESSION_CLICKED to { startEditWordSession(it) },
        TelegramConfig.CallbackData.CallbacksEnum.DELETE_WORD_CLICKED to { handleDeleteWord(it) },
        TelegramConfig.CallbackData.CallbacksEnum.START_LEARNING_CLICKED to { startLearningMode(it) },
        TelegramConfig.CallbackData.CallbacksEnum.DELETE_DICTIONARY_CLICKED to { handleDeleteAllWords(it) },
        TelegramConfig.CallbackData.CallbacksEnum.STATISTICS_CLICKED to { handleShowStatistics(it) },
        TelegramConfig.CallbackData.CallbacksEnum.RESET_CLICKED to { handleResetStatistics(it) },
        TelegramConfig.CallbackData.CallbacksEnum.DELETE_HINT_CLICKED to { handleDeleteHint(it) },
        TelegramConfig.CallbackData.CallbacksEnum.GO_BACK_TO_WORD_MENU_CLICKED to { handleReturnToWordEditorMenu(it) },
        TelegramConfig.CallbackData.CallbacksEnum.RETURN_MAIN_MENU_CLICKED to { handleReturnToMainMenu(it) },
    )

    private val nonCleanupCallbacks =
        setOf(
            TelegramConfig.CallbackData.CallbacksEnum.RESET_CLICKED,
            TelegramConfig.CallbackData.CallbacksEnum.STATISTICS_CLICKED,
            TelegramConfig.CallbackData.CallbacksEnum.DELETE_HINT_CLICKED
        )

    fun handleCallbackData(data: String, chatId: Long) {
        if (data.isEmpty()) return
        if (data.startsWith(TelegramConfig.CallbackData.ANSWER_PREFIX)) {
            handleAnswerSubmission(data, chatId)
            return
        }

        messenger.runWithMessagesCleanup(chatId, data, nonCleanupCallbacks) { callback ->
            callbackActions[callback]?.invoke(chatId)
        }
    }

    fun sendMessageByKeyWord(keyWord: String, chatId: Long) {
        val keyWord = TelegramConfig.KeyWords.Companion.getKeyWordFromKey(keyWord)
        when (keyWord) {
            TelegramConfig.KeyWords.INFO -> {
                messenger.sendSimpleTextAndSave(chatId, INFO_ADD_WORDS)
            }

            TelegramConfig.KeyWords.START -> {
                clearAllState(chatId)
                messenger.sendAndSave(chatId) { service.sendMainMenu(chatId) }
            }

            null -> return
        }
    }

    internal fun handleAddWordsFromFile(chatId: Long, document: Document?) {
        val session = dataStorage.getSession(chatId)
        val currentWords = dataStorage.getCurrentWords(chatId)

        if (document != null && session == null && currentWords.isEmpty())
            addWordsFromFile(chatId, document)
    }

    internal fun showWordEditorMenu(chatId: Long, selectedWord: Word?) {
        selectedWord?.let { word ->
            sendImageIfExists(chatId, word.originalWord, false)
            messenger.sendAndSave(chatId) {
                service.sendWordEditorMenu(chatId, "${word.originalWord} - ${word.translatedWord}")
            }
        }
    }

    private fun handleReturnToWordEditorMenu(chatId: Long) {
        val currentWord = dataStorage.getCurrentWord(chatId)
        dataStorage.clearSession(chatId)
        showWordEditorMenu(chatId, currentWord)
    }

    private fun handleReturnToMainMenu(chatId: Long) {
        clearAllState(chatId)
        showMainMenu(chatId)
    }

    private fun addWordsFromFile(chatId: Long, document: Document) {
        val isWordAdded = filesHelper.addWordsFromFile(document, chatId)
        if (isWordAdded) {
            messenger.deletePrevious(chatId)
            messenger.sendAndSave(chatId) { service.sendMessage(chatId, WORDS_ADDED) }
            startSelectWordSession(chatId)
        } else {
            messenger.sendSimpleTextAndSave(chatId, WORDS_NOT_ADDED)
        }
    }

    private fun startSelectWordSession(chatId: Long) {
        dataStorage.setCurrentWord(chatId, null)
        dataStorage.setCurrentWords(chatId, repository.getDictionary(chatId))
        val currentWords = dataStorage.getCurrentWords(chatId)

        if (currentWords.isEmpty()) {
            messenger.sendAndSave(chatId) {
                service.sendShowWordsMenu(chatId, DICTIONARY_IS_EMPTY, true)
            }
            return
        }

        val dictionaryString =
            currentWords.mapIndexed { index, word -> "${index + 1}. ${word.originalWord} - ${word.translatedWord}" }
                .joinToString("\n")

        messenger.sendAndSave(chatId) {
            service.sendShowWordsMenu(chatId, "${NUM_OF_WORD_REQUESTED}\n\n$dictionaryString")
        }

        dataStorage.setSession(chatId, SessionStorage.Sessions.SelectWordSession)
    }

    private fun startSetPictureSession(chatId: Long) {
        dataStorage.setSession(chatId, SessionStorage.Sessions.AddPictureSession)
        showSetPictureMenu(chatId)
    }

    private fun startAddWordsSession(chatId: Long) {
        messenger.sendAndSave(chatId) {
            service.sendWordsRequest(
                chatId,
                ORIGINAL_WORD_REQUEST
            )
        }
        dataStorage.setSession(chatId, SessionStorage.Sessions.AddWordsSession())
    }

    private fun handleDeleteWord(chatId: Long) {
        val word: String = dataStorage.getCurrentWord(chatId)?.originalWord ?: ""
        repository.deleteWord(chatId, word)

        messenger.sendSimpleTextAndSave(chatId, WORD_DELETED.format(word))
        startSelectWordSession(chatId)
    }


    private fun showSetPictureMenu(chatId: Long) {
        messenger.sendAndSave(chatId) {
            service.sendAddPictureMenu(chatId, SET_PICTURE_REQUEST)
        }
    }

    private fun startEditWordSession(chatId: Long) {
        val word = dataStorage.getCurrentWord(chatId) ?: return

        val text =
            "${ORIGINAL_WORD_REQUEST}\n>> ${word.originalWord.uppercase()} << ${word.translatedWord}"
        messenger.sendAndSave(chatId) { service.sendWordsRequest(chatId, text, false) }

        dataStorage.setSession(chatId, SessionStorage.Sessions.EditWordSession())
    }

    private fun startLearningMode(chatId: Long) {
        dataStorage.setSession(chatId, SessionStorage.Sessions.LearnWordsSession)
        checkNextQuestionAndSend(chatId)
    }

    private fun handleDeleteAllWords(chatId: Long) {
        repository.clearUsersWords(chatId)
        dataStorage.clearAll(chatId)

        messenger.sendSimpleTextAndSave(chatId, ALL_WORDS_DELETED)
        messenger.sendAndSave(chatId) { service.sendShowWordsMenu(chatId, DICTIONARY_IS_EMPTY, true) }
    }

    private fun showMainMenu(chatId: Long) {
        messenger.sendAndSave(chatId) { service.sendMainMenu(chatId) }
    }

    private fun handleResetStatistics(chatId: Long) {
        trainer.resetStatistics(chatId)
        val statistics = trainer.getStatistics(chatId)
        val text = "${STATISTICS_RESET}\n${
            STATISTIC_FORMAT.format(
                statistics.learnedWordsCount,
                statistics.totalWordsCount,
                statistics.learnedWordsPercent
            )
        }"
        messenger.sendSimpleTextAndSave(chatId, text)
    }

    private fun handleShowStatistics(chatId: Long) {
        val statistics = trainer.getStatistics(chatId)
        val text = STATISTIC_FORMAT
            .format(statistics.learnedWordsCount, statistics.totalWordsCount, statistics.learnedWordsPercent)

        messenger.sendSimpleTextAndSave(chatId, text)
    }

    private fun handleAnswerSubmission(data: String, chatId: Long) {
        val indexOfClicked = data.substringAfter(TelegramConfig.CallbackData.ANSWER_PREFIX).toInt()
        val isRightAnswer = trainer.checkAnswer(indexOfClicked, chatId)

        val correctAnswer = trainer.question?.correctAnswer
        val wordsMessage = "${correctAnswer?.originalWord} - ${correctAnswer?.translatedWord}"

        val text =
            if (isRightAnswer) "$WORD_CORRECT_ANSWER $wordsMessage"
            else "$WORD_INCORRECT_ANSWER $wordsMessage"

        messenger.deletePrevious(chatId)
        messenger.sendSimpleTextAndSave(chatId, text)
        checkNextQuestionAndSend(chatId)
    }

    private fun checkNextQuestionAndSend(chatId: Long) {
        val question = trainer.getNextQuestion(chatId)
        if (question == null) {
            dataStorage.clearSession(chatId)
            sendCompletionMessage(chatId)
        } else {
            val originalWord = question.correctAnswer.originalWord
            sendImageIfExists(chatId, originalWord, true)
            sendQuestion(chatId, question)
        }
    }

    private fun sendCompletionMessage(chatId: Long) {
        messenger.sendSimpleTextAndSave(chatId, LEARNING_COMPLETED)
        messenger.sendAndSave(chatId) { service.sendMainMenu(chatId) }
    }

    private fun sendQuestion(chatId: Long, question: Question) {
        messenger.sendAndSave(chatId) { service.sendQuestion(chatId, question) }
    }

    private fun sendImageIfExists(chatId: Long, originalWord: String, hasSpoiler: Boolean) {
        val fileId = repository.getPhotoFileId(chatId, originalWord) ?: return
        messenger.sendAndSave(chatId) { service.sendPhotoByFileId(fileId, chatId, hasSpoiler) }
    }

    private fun handleDeleteHint(chatId: Long) {
        val currentWord = dataStorage.getCurrentWord(chatId) ?: return
        val isDeleted = deleteHintIfExists(chatId)
        val hintDeleted = if (isDeleted) HINT_DELETED else HINT_NOT_DELETED
        messenger.sendSimpleTextAndSave(chatId, hintDeleted)

        if (isDeleted) {
            messenger.deletePrevious(chatId)
            messenger.sendSimpleTextAndSave(chatId, hintDeleted)
            showWordEditorMenu(chatId, currentWord)
        }
    }

    private fun deleteHintIfExists(chatId: Long): Boolean {
        val currentWord: String = dataStorage.getCurrentWord(chatId)?.originalWord ?: return false

        val isFileIdExist = repository.checkFileIdExistence(chatId, currentWord)
        if (isFileIdExist) repository.updateFileId(chatId, null, currentWord)

        return isFileIdExist
    }

    private fun clearAllState(chatId: Long) {
        messenger.deletePrevious(chatId)
        dataStorage.clearAll(chatId)
    }
}