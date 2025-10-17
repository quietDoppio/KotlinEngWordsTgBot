package serializableClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramResponse<T>(
    val ok: Boolean,
    val result: T? = null
)

@Serializable
data class BotUpdate(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
    val message: Message? = null
)

@Serializable
data class CallbackQuery(
    val data: String? = null,
    val message: Message? = null,
    val from: From
)

@Serializable
data class Message(
    val text: String? = null,
    @SerialName("message_id")
    val messageId: Long,
    val chat: Chat,
    val from: From,
    val document: Document? = null,
    val photo: List<PhotoSize>? = null
)

@Serializable
data class Chat(
    @SerialName("id")
    val chatId: Long,
    val username: String? = null,
    val title: String? = null
)

@Serializable
data class From(
    val username: String? = null
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
    val filePath: String? = null
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
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Int,
    val width: Int,
    val height: Int
)
