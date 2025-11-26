package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileTrackCrossRef
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileTrackCrossRefDao
import kotlinx.coroutines.flow.Flow

class MediaFileTrackCrossRefRepo(
    private val dao: MediaFileTrackCrossRefDao
) {
    fun getAll(): Flow<List<MediaFileTrackCrossRef>> {
        return dao.getAll()
    }

    suspend fun add(mediaFileId: MediaFileId, trackId: TrackId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(mediaFileId: MediaFileId, trackId: TrackId) {
        dao.delete(mediaFileId = mediaFileId, trackId = trackId)
    }
}