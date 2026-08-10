package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.common.data.room.daos.TagTrackCrossRefDao

class TagTrackCrossRefRepo(
    private val dao: TagTrackCrossRefDao
) {
    suspend fun add(tagId: TagId, trackId: TrackId) {
        dao.add(
            tagId = tagId,
            trackId = trackId,
            creationDatetime = System.currentTimeMillis()
        )
    }

    suspend fun delete(tagId: TagId, trackId: TrackId) {
        dao.delete(
            tagId = tagId,
            trackId = trackId
        )
    }
}