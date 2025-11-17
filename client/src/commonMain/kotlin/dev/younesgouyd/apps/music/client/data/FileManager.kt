package dev.younesgouyd.apps.music.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileManager(
    appDir: File
) {
    private val mediaDir = File(appDir, "media").also { it.mkdir() }
    private val inspectionDir = File(appDir, "inspections").also { it.mkdir() }

    suspend fun saveMediaFile(data: InputStream, id: MediaFileId) {
        withContext(Dispatchers.IO) {
            File(mediaDir, id.toString())
                .outputStream()
                .use { fileStream -> data.copyTo(fileStream) }
        }
    }

    suspend fun saveMediaFile(data: ByteArray, id: MediaFileId) {
        withContext(Dispatchers.IO) {
            File(mediaDir, id.toString())
                .writeBytes(data)
        }
    }

    fun getMediaFile(id: MediaFileId): File {
        return File(mediaDir, id.toString())
    }

    suspend fun saveYtDlpInspection(importSessionId: ImportSessionId, inspection: String) {
        withContext(Dispatchers.IO) {
            File(inspectionDir, importSessionId.toString())
                .writeText(inspection)
        }
    }
}