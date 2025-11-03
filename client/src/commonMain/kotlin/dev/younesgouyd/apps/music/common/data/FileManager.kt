package dev.younesgouyd.apps.music.common.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager(
    appDir: File
) {
    val mediaDir = File(appDir, "media").also { it.mkdir() }

    suspend fun saveMediaFile(data: ByteArray, id: Long) {
        withContext(Dispatchers.IO) {
            File(mediaDir, id.toString()).writeBytes(data)
        }
    }

    fun getMediaFile(id: Long): File {
        return File(mediaDir, id.toString())
    }
}