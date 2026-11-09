package dev.younesgouyd.apps.music.server.multiplatform.data

import dev.younesgouyd.apps.music.common.models.MediaFileId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

class FileManager(
    private val mediaDir: File,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun saveMediaFile(channel: ByteReadChannel, id: MediaFileId) {
        channel.copyAndClose(
            File(mediaDir, id.toString()).writeChannel()
        )
    }

    suspend fun saveMediaFile(data: ByteArray, id: MediaFileId) {
        withContext(Dispatchers.IO) {
            File(mediaDir, id.toString())
                .writeBytes(data)
        }
    }

    // TODO: check existence
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

    suspend fun rollback(id: MediaFileId) {
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(mediaDir, id.toString())
                    if (file.exists()) {
                        file.deleteRecursively()
                    }
                } catch (e: Exception) {
                    logger.error(e) { "could not rollback file with id: $id" }
                }
            }
        }
    }
}