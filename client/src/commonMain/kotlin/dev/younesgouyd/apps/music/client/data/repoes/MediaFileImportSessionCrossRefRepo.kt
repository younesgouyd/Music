package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileImportSessionCrossRefDao

class MediaFileImportSessionCrossRefRepo(
    private val dao: MediaFileImportSessionCrossRefDao
) {
    suspend fun add(mediaFileId: MediaFileId, importSessionId: ImportSessionId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            importSessionId = importSessionId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: MediaFileId, importSessionId: ImportSessionId) {
        dao.delete(mediaFileId = mediaFileId, importSessionId = importSessionId)
    }
}