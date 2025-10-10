package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.common.data.room.entities.PlaylistTrackCrossRefDao
import kotlinx.coroutines.flow.Flow

class PlaylistTrackCrossRefRepo(private val dao: PlaylistTrackCrossRefDao) {
    suspend fun add(playlistId: Long, trackId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            playlistId = playlistId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    fun get(playlistId: Long, trackId: Long): Flow<PlaylistTrackCrossRef?> {
        return dao.get(playlistId, trackId)
    }

    suspend fun delete(playlistId: Long, trackId: Long) {
        dao.delete(playlistId, trackId)
    }
}