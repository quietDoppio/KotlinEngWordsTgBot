package Utils

import api.TelegramApiService

interface FilesHelper {
    fun downloadFile(apiFilePath: String, fileName: String)
    fun getFileRequest(fileId: String): String
}

class TelegramFilesHelperImpl(
    private val api: TelegramApiService,
) : FilesHelper {
    override fun downloadFile(apiFilePath: String, fileName: String) {
        api.downloadFile(apiFilePath, fileName)
    }

    override fun getFileRequest(fileId: String): String {
        return api.getFileRequest(fileId)
    }

}