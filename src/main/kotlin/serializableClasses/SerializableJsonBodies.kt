package serializableClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageBody(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String?,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class CopyMessageBody(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("from_chat_id")
    val fromChatId: Long,
    @SerialName("message_id")
    val messageId: Long,
)

@Serializable
data class DeleteMessageBody(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("message_ids")
    val messageIds: List<Long>
)

@Serializable
data class EditMessageBody(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup
)

@Serializable
data class GetFileBody(
    @SerialName("file_id")
    val fileId: String,
)

@Serializable
data class SendPhotoBody(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("photo")
    val fileId: String,
    @SerialName("has_spoiler")
    val hasSpoiler: Boolean
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboardButton?>>
)

@Serializable
data class InlineKeyboardButton(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String
)