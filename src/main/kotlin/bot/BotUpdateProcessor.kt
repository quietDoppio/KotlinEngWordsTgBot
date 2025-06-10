package bot

import bot.serializableClasses.GetFileResponse
import bot.serializableClasses.MessageResponse
import bot.serializableClasses.Response
import bot.serializableClasses.Update
import deprecated.STATISTIC_TO_SEND
import kotlinx.serialization.json.Json
import java.io.File

class BotUpdateProcessor(
    private val botService: TelegramBotService,
    private val trainer: LearnWordsTrainer,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    val wordSessionState = mutableMapOf<Long, WordSessionState?>()
    val chatAndMessagesIds = mutableMapOf<Long, Long>()
    var lastUpdateId: Long = 0

    fun run() {
        while (true) {
            Thread.sleep(2000)
            val responseString = botService.getUpdates(lastUpdateId)
            val response = json.decodeFromString<Response>(responseString)
            if (response.updates.isEmpty()) continue

            val updates = response.updates.sortedBy { it.updateId }
            lastUpdateId = updates.last().updateId + 1
            updates.forEach { handleUpdate(it) }
        }
    }

    private fun handleUpdate(update: Update) {
        val chatId = update.message?.chat?.chatId ?: update.callbackQuery?.message?.chat?.chatId ?: return
        val message = update.message?.text
        val document = update.message?.document
        val data = update.callbackQuery?.data ?: ""
        val session = wordSessionState[chatId]
        val username = update.message?.from?.username ?: update.callbackQuery?.from?.username ?: "unknown_user"
        val messageId = chatAndMessagesIds[chatId] ?: 0L

        trainer.repository.addUser(chatId, username)

        if (message == "/start") {
            if (wordSessionState[chatId] != null) wordSessionState[chatId] = null
            botService.sendMainMenu(json, chatId)
            println(chatId)
        }

        if (session != null && data != Constants.CALLBACK_DATA_RETURN_CLICKED && message != "/start") {
            when (session.waitingFor) {
                WaitingFor.ORIGINAL -> {
                    session.currentOriginal = message ?: ""
                    session.waitingFor = WaitingFor.TRANSLATION

                    println(botService.editMessage(chatId, messageId, "Введите перевод ${Constants.EMOJI_RU_FLAG}:", json))
                    //botService.sendNewWordsRequest(json, chatId, "Введите перевод $bot.RU_EMOJI:")
                }

                WaitingFor.TRANSLATION -> {
                    session.waitingFor = WaitingFor.ORIGINAL

                    val original = session.currentOriginal
                    val translation = message ?: ""
                    val wordToAdd = Word(originalWord = original, translatedWord = translation)
                    trainer.repository.addWords(chatId, listOf(wordToAdd))
                    trainer.repository.addUserAnswers(chatId, listOf(wordToAdd))
                    botService.editMessage(
                        chatId, messageId, "Введите  оригинал ${Constants}:", json
                    )
                    //botService.sendNewWordsRequest(json, chatId, "Введите оригинал $bot.ENG_EMOJI:")
                }
            }
            return
        }

        if (document != null) {
            println()
            val jsonResponse = botService.getFileRequest(document.fileId, json)
            val response: GetFileResponse = json.decodeFromString(jsonResponse)
            response.result?.filePath?.let { filePath ->
                botService.downloadFile(filePath, document.fileName)
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
            trainer.repository.addWords(chatId, newWords)

        }

        when {
            Constants.CALLBACK_DATA_START_LEARNING_CLICKED == data -> checkNextQuestionAndSend(chatId)
            Constants.CALLBACK_DATA_RETURN_CLICKED == data -> {
                botService.sendMainMenu(json, chatId)
                if (wordSessionState[chatId] != null) wordSessionState[chatId] = null
            }

            Constants.CALLBACK_DATA_RESET_CLICKED == data -> {
                trainer.resetStatistics(chatId)
                botService.sendMessage(json, chatId, "Статистика успешно сброшена")
                botService.sendMainMenu(json, chatId)
            }

            Constants.CALLBACK_DATA_ADD_WORDS == data -> {
                val response = botService.sendNewWordsRequest(json, chatId, "Введите оригинал ${Constants.EMOJI_ENG_FLAG}:")
                println("Запрос слов - $response")
                chatAndMessagesIds[chatId] = json.decodeFromString<MessageResponse>(response).result?.messageId ?: 0L
                wordSessionState[chatId] = WordSessionState()
            }

            Constants.CALLBACK_DATA_STATISTICS_CLICKED == data -> {
                val statistics = trainer.getStatistics(chatId)
                botService.sendMessage(
                    json,
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

                if (isRightAnswer) botService.sendMessage(json, chatId, "${Constants.EMOJI_CHECKMARK} Верно!")
                else botService.sendMessage(
                    json,
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
            botService.sendMessage(
                json,
                chatId,
                "${Constants.EMOJI_HUNDRED} Вы выучили все слова в базе или они отсутствуют"
            )
        } else {
            val originalWord = question.correctAnswer.originalWord
            if (trainer.repository.checkFileIdExistence(originalWord)) {
                trainer.repository.getFileId(originalWord)?.let { fileId ->
                    println(fileId)
                    botService.sendPhotoByFileId(fileId = fileId, chatId = chatId, hasSpoiler = true, json = json)
                }
            } else {
                val file = File("build/libs/$originalWord.png")
                if (file.exists()) {
                    val sendPhotoResponse = botService.sendPhotoByFile(file, chatId, true)
                    val response: MessageResponse = json.decodeFromString(sendPhotoResponse)
                    val photoFileId = response.result?.photos?.find { it.width == 320 }?.fileId
                    if (photoFileId != null) trainer.repository.updateFileId(photoFileId, originalWord)
                }
            }
            botService.sendQuestion(json, chatId, question)
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