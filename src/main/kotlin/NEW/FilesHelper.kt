package NEW

import kotlinx.serialization.json.Json
import java.net.http.HttpRequest

interface FilesHelper {
    fun downloadFile(apiFilePath: String, fileName: String)
    fun getFileRequest(fileId: String): String
}