package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileTrackCrossRefDao

class MediaFileTrackCrossRefRepo(
    private val dao: MediaFileTrackCrossRefDao
) {
    suspend fun add(mediaFileId: Long, trackId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(mediaFileId: Long, trackId: Long) {
        dao.delete(mediaFileId = mediaFileId, trackId = trackId)
    }
}