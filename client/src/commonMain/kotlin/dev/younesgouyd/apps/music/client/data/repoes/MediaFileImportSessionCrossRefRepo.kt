package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileImportSessionCrossRefDao

class MediaFileImportSessionCrossRefRepo(
    private val dao: MediaFileImportSessionCrossRefDao
) {
    suspend fun add(mediaFileId: Long, importSessionId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            importSessionId = importSessionId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: Long, importSessionId: Long) {
        dao.delete(mediaFileId = mediaFileId, importSessionId = importSessionId)
    }
}