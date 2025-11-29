package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.TagTrackCrossRefDao

class TagTrackCrossRefRepo(
    private val dao: TagTrackCrossRefDao
) {
    suspend fun add(tagId: TagId, trackId: TrackId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            tagId = tagId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(tagId: TagId, trackId: TrackId) {
        dao.delete(
            tagId = tagId,
            trackId = trackId
        )
    }
}