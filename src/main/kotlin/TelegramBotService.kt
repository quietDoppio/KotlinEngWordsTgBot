import dictionary.Question
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val API_TELEGRAM_URL = "https://api.telegram.org/bot"
const val CALLBACK_DATA_STATISTICS_CLICKED = "DATA_CALLBACK_STATISTICS"
const val CALLBACK_DATA_START_LEARNING_CLICKED = "DATA_CALLBACK_START_LEARNING"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(botToken: String) {

    private val botUrl = API_TELEGRAM_URL + botToken
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val request: HttpRequest = makeRequest("$botUrl/getUpdates?offset=$updateId")
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, messageToSend: String): String {
        val encoded = URLEncoder.encode(messageToSend, StandardCharsets.UTF_8)
        val message = encoded.replace(" ", "+")
        val request: HttpRequest = makeRequest("$botUrl/sendMessage?chat_id=$chatId&text=$message")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMainMenu(chatId: Long): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val menuJsonBody = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Начать изучение",
                      "callback_data": "$CALLBACK_DATA_START_LEARNING_CLICKED"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "$CALLBACK_DATA_STATISTICS_CLICKED"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()
        val postJsonRequest: HttpRequest = makePostJsonRequest(sendMessageUrl, menuJsonBody)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val sendMessageUrl = "${botUrl}/sendMessage"
        val questionJsonBody = """
            {
              "chat_id": $chatId,
              "text": "${question.correctAnswer.originalWord}",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "${question.variants[0].translatedWord}",
                      "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}0"
                    },                   
                    {
                      "text": "${question.variants[1].translatedWord}",
                      "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}1"
                    },
                    {
                      "text": "${question.variants[2].translatedWord}",
                      "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}2"
                    },
                    {
                      "text": "${question.variants[3].translatedWord}",
                      "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}3"
                    }                              
                  ]
                ]
              }
            }
        """.trimIndent()
        val postJsonRequest: HttpRequest = makePostJsonRequest(sendMessageUrl, questionJsonBody)
        val response: HttpResponse<String> = client.send(postJsonRequest, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    private fun makeRequest(botUrl: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(botUrl)).build()

    private fun makePostJsonRequest(botUrl: String, jsonBody: String): HttpRequest {
        return HttpRequest.newBuilder().uri(URI.create(botUrl))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()
    }
}