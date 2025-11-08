package dev.younesgouyd.apps.music.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileManager(
    appDir: File
) {
    val mediaDir = File(appDir, "media").also { it.mkdir() }

    suspend fun saveMediaFile(data: InputStream, id: Long) {
        withContext(Dispatchers.IO) {
            File(mediaDir, id.toString())
                .outputStream()
                .use { fileStream -> data.copyTo(fileStream) }
        }
    }

    fun getMediaFile(id: Long): File {
        return File(mediaDir, id.toString())
    }
}