package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream

class MediaFileRepo(
    private val dao: MediaFileDao,
    private val fileManager: FileManager
) {
    suspend fun getMediaFileUri(id: Long): String {
        val mediaFile = dao.getTrackMediaFiles(id)
            .map { it.first() }
            .first()
        return fileManager.getMediaFile(mediaFile.id)
            .toPath()
            .toUri()
            .toString()
    }

    suspend fun add(trackId: Long, importSessionItemId: Long, data: InputStream): Long {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            trackId = trackId,
            importSessionItemId = importSessionItemId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        fileManager.saveMediaFile(data, id)
        return id
    }
}