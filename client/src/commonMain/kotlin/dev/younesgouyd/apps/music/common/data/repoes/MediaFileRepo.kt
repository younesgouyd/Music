package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.FileManager
import dev.younesgouyd.apps.music.common.data.room.entities.MediaFileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    suspend fun add(trackId: Long, importSessionItemId: Long, data: ByteArray): Long {
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