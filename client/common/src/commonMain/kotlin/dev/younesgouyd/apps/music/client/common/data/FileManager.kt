package dev.younesgouyd.apps.music.client.common.data

import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileManager(
    private val tempDir: File
) {
    suspend fun storeTemp(id: MediaFileId, stream: InputStream): File {
        val file = File(tempDir, id.toString())
        withContext(Dispatchers.IO) {
            stream.use { streamIn ->
                file.outputStream().use { streamOut ->
                    streamIn.copyTo(streamOut)
                }
            }
        }
        return file
    }

    suspend fun clearTemp() {
        withContext(Dispatchers.IO) {
            tempDir.listFiles().orEmpty().forEach {
                it.delete()
            }
        }
    }
}