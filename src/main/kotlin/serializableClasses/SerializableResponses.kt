package serializableClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("result")
    val updates: List<BotUpdate>
)

@Serializable
data class MessageResponse(
    val ok: Boolean,
    val result: Message? = null,
)

@Serializable
data class GetFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null,
)

@Serializable
data class BotUpdate(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Int,
    @SerialName("width")
    val width: Int,
    @SerialName("height")
    val height: Int
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("from")
    val from: From
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("from")
    val from: From,
    @SerialName("document")
    val document: Document? = null,
    @SerialName("photo")
    val photo: List<PhotoSize>? = null
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String? = null,
    @SerialName("file_unique_id")
    val fileUniqueId: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("file_path")
    val filePath: String? = null,
)

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long
)

@Serializable
data class From(
    @SerialName("username")
    val username: String? = null
)

@Serializable
data class Chat(
    @SerialName("id")
    val chatId: Long,
    @SerialName("username")
    val username: String? = null,
    @SerialName("title")
    val title: String? = null
)

