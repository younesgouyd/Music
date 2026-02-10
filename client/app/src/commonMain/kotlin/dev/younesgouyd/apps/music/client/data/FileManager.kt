package dev.younesgouyd.apps.music.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileManager(
    appDir: File,
    val dbDir: File,
) {
    val mediaDir = File(appDir, "media").also { it.mkdir() }
    val inspectionDir = File(appDir, "inspections").also { it.mkdir() }

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

    suspend fun delete(ids: Set<MediaFileId>) {
        withContext(Dispatchers.IO) {
            for (id in ids) {
                var success = false
                val file = File(mediaDir, id.toString())
                if (file.exists()) {
                    success = file.delete()
                }
                if (success) {
                    println("FileManager::delete | deleted: ${file.path}") // TODO
                } else {
                    println("FileManager::delete | Failed to delete: ${file.path}") // TODO
                }
            }
        }
    }
}