package bot

import NEW.BotRequestSender
import NEW.FilesHelper

import NEW.UpdateSource
import NEW.UserCommandProcessor
import bot.serializableClasses.GetFileResponse
import bot.serializableClasses.MessageResponse
import bot.serializableClasses.BotUpdate
import deprecated.STATISTIC_TO_SEND
import kotlinx.serialization.json.Json
import java.io.File

class BotUpdateProcessor(
    private val updatesSource: UpdateSource,
    private val requestSender: BotRequestSender,
    private val filesHelper: FilesHelper,
    private val trainer: LearnWordsTrainer,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : UserCommandProcessor {
    val wordSessionState = mutableMapOf<Long, WordSessionState?>()
    val chatAndMessagesIds = mutableMapOf<Long, Long>()
    var lastUpdateId: Long = 0

    override fun run() {
        while (true) {
            Thread.sleep(2000)
            val updates = updatesSource.getUpdates(lastUpdateId)
            if (updates.isEmpty()) continue

            val sortedUpdates = updates.sortedBy { it.updateId }
            lastUpdateId = sortedUpdates.last().updateId + 1
            updates.forEach { handleUpdate(it) }
        }
    }

    override fun handleUpdate(update: BotUpdate) {
        val chatId = update.message?.chat?.chatId ?: update.callbackQuery?.message?.chat?.chatId ?: return
        val message = update.message?.text
        val document = update.message?.document
        val data = update.callbackQuery?.data ?: ""
        val session = wordSessionState[chatId]
        val username = update.message?.from?.username ?: update.callbackQuery?.from?.username ?: "unknown_user"
        val messageId = chatAndMessagesIds[chatId] ?: 0L

        trainer.repository.addNewUser(chatId, username)

        if (message == "/start") {
            if (wordSessionState[chatId] != null) wordSessionState[chatId] = null
            requestSender.sendMainMenu(chatId)
            println(chatId)
        }

        if (session != null && data != Constants.CALLBACK_DATA_RETURN_CLICKED && message != "/start") {
            when (session.waitingFor) {
                WaitingFor.ORIGINAL -> {
                    session.currentOriginal = message ?: ""
                    session.waitingFor = WaitingFor.TRANSLATION

                    println(
                        requestSender.editMessage(
                            chatId, messageId, "Введите перевод ${Constants.EMOJI_RU_FLAG}:",
                        )
                    )
                    //botService.sendNewWordsRequest(json, chatId, "Введите перевод $bot.RU_EMOJI:")
                }

                WaitingFor.TRANSLATION -> {
                    session.waitingFor = WaitingFor.ORIGINAL

                    val original = session.currentOriginal
                    val translation = message ?: ""
                    val wordToAdd = Word(originalWord = original, translatedWord = translation)
                    trainer.repository.addWordsToUser(chatId, listOf(wordToAdd))
                    trainer.repository.addUserAnswersToUser(chatId, listOf(wordToAdd))
                    requestSender.editMessage(chatId, messageId, "Введите  оригинал ${Constants}:")
                    //botService.sendNewWordsRequest(json, chatId, "Введите оригинал $bot.ENG_EMOJI:")
                }
            }
            return
        }

        if (document != null) {
            println()
            val jsonResponse = filesHelper.getFileRequest(document.fileId)
            val response: GetFileResponse = json.decodeFromString(jsonResponse)
            response.result?.filePath?.let { filePath ->
                filesHelper.downloadFile(filePath, document.fileName)
            }
            println(jsonResponse)
            val newWords: MutableList<Word> = mutableListOf()
            File(document.fileName).forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val text = parts[0].trim()
                    val translate = parts[1].trim()
                    newWords.add(Word(originalWord = text, translatedWord = translate))
                    val endsWithNewLine: Boolean = File("words.txt").readText().endsWith("\n")
                    val lineToAppend = "${text}|${translate}"
                    val correctLine = if (endsWithNewLine) lineToAppend else "\n$lineToAppend"
                    File("words.txt").appendText(correctLine)
                }
            }
            trainer.repository.addWordsToUser(chatId, newWords)

        }

        when {
            Constants.CALLBACK_DATA_START_LEARNING_CLICKED == data -> checkNextQuestionAndSend(chatId)
            Constants.CALLBACK_DATA_RETURN_CLICKED == data -> {
                requestSender.sendMainMenu(chatId)
                if (wordSessionState[chatId] != null) wordSessionState[chatId] = null
            }

            Constants.CALLBACK_DATA_RESET_CLICKED == data -> {
                trainer.resetStatistics(chatId)
                requestSender.sendMessage(chatId, "Статистика успешно сброшена")
                requestSender.sendMainMenu(chatId)
            }

            Constants.CALLBACK_DATA_ADD_WORDS == data -> {
                val response =
                    requestSender.sendNewWordsRequest(chatId, "Введите оригинал ${Constants.EMOJI_ENG_FLAG}:")
                println("Запрос слов - $response")
                chatAndMessagesIds[chatId] = json.decodeFromString<MessageResponse>(response).result?.messageId ?: 0L
                wordSessionState[chatId] = WordSessionState()
            }

            Constants.CALLBACK_DATA_STATISTICS_CLICKED == data -> {
                val statistics = trainer.getStatistics(chatId)
                requestSender.sendMessage(
                    chatId,
                    STATISTIC_TO_SEND.format(
                        statistics.learnedWordsCount,
                        statistics.totalWordsCount,
                        statistics.learnedWordsPercent
                    )
                )
            }

            data.startsWith(Constants.CALLBACK_DATA_ANSWER_PREFIX) -> {
                val indexOfClicked = data.substringAfter(Constants.CALLBACK_DATA_ANSWER_PREFIX).toInt()
                val isRightAnswer = trainer.checkAnswer(indexOfClicked, chatId)

                if (isRightAnswer) requestSender.sendMessage(chatId, "${Constants.EMOJI_CHECKMARK} Верно!")
                else requestSender.sendMessage(

                    chatId,
                    "${Constants.EMOJI_CROSSMARK} Не верно! ${trainer.question?.correctAnswer?.originalWord} - ${trainer.question?.correctAnswer?.translatedWord}"
                )
                checkNextQuestionAndSend(chatId)
            }

        }
    }

    private fun checkNextQuestionAndSend(chatId: Long) {
        val question = trainer.getNextQuestion(chatId)
        if (question == null) {
            requestSender.sendMessage(
                chatId,
                "${Constants.EMOJI_HUNDRED} Вы выучили все слова в базе или они отсутствуют"
            )
        } else {
            val originalWord = question.correctAnswer.originalWord
            if (trainer.repository.checkFileIdExistence(originalWord)) {
                trainer.repository.getFileId(originalWord)?.let { fileId ->
                    println(fileId)
                    requestSender.sendPhotoByFileId(fileId = fileId, chatId = chatId, hasSpoiler = true)
                }
            } else {
                val file = File("build/libs/$originalWord.png")
                if (file.exists()) {
                    val sendPhotoResponse = requestSender.sendPhotoByFile(file, chatId, true)
                    val response: MessageResponse = json.decodeFromString(sendPhotoResponse)
                    val photoFileId = response.result?.photos?.find { it.width == 320 }?.fileId
                    if (photoFileId != null) trainer.repository.updateFileId(photoFileId, originalWord)
                }
            }
            requestSender.sendQuestion(chatId, question)
        }

    }
}

data class WordSessionState(
    var waitingFor: WaitingFor = WaitingFor.ORIGINAL,
    var currentOriginal: String = ""
)

enum class WaitingFor {
    ORIGINAL,
    TRANSLATION
}