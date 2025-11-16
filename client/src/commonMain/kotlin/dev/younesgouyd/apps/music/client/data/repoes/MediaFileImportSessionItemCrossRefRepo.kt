package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileImportSessionItemCrossRefDao

class MediaFileImportSessionItemCrossRefRepo(
    private val dao: MediaFileImportSessionItemCrossRefDao
) {
    suspend fun add(mediaFileId: Long, importSessionItemId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            importSessionItemId = importSessionItemId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(mediaFileId: Long, importSessionItemId: Long) {
        dao.delete(mediaFileId = mediaFileId, importSessionItemId = importSessionItemId)
    }
}