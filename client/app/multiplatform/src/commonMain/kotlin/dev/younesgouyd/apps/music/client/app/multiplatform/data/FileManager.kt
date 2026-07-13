package dev.younesgouyd.apps.music.client.app.multiplatform.data

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileManager(
    appDir: File,
    val dbDir: File,
) {
    private val logger = KotlinLogging.logger {}
    val mediaDir = File(appDir, "media").also { it.mkdir() }
    val logsDir = File(appDir, "logs").also { it.mkdir() }

    suspend fun saveMediaFile(data: InputStream, id: MediaFileId) {
        withContext(Dispatchers.IO) {
            data.copyTo(File(mediaDir, id.toString()).outputStream())
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

    suspend fun delete(ids: Set<MediaFileId>) {
        withContext(Dispatchers.IO) {
            for (id in ids) {
                var success = false
                val file = File(mediaDir, id.toString())
                if (file.exists()) {
                    success = file.delete()
                }
                if (success) {
                    logger.info { "::delete | deleted: ${file.path}" } // TODO
                } else {
                    logger.info { "::delete | Failed to delete: ${file.path}" } // TODO
                }
            }
        }
    }
}