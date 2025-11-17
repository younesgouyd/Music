package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileImportSessionItemCrossRefDao

class MediaFileImportSessionItemCrossRefRepo(
    private val dao: MediaFileImportSessionItemCrossRefDao
) {
    suspend fun add(mediaFileId: MediaFileId, importSessionItemId: ImportSessionItemId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            importSessionItemId = importSessionItemId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(mediaFileId: MediaFileId, importSessionItemId: ImportSessionItemId) {
        dao.delete(mediaFileId = mediaFileId, importSessionItemId = importSessionItemId)
    }
}