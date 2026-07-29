package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.PlaylistId
import dev.younesgouyd.apps.music.common.TrackId
import dev.younesgouyd.apps.music.server.common.data.room.daos.PlaylistTrackCrossRefDao
import dev.younesgouyd.apps.music.server.common.data.room.entities.PlaylistTrackCrossRef
import kotlinx.coroutines.flow.Flow

class PlaylistTrackCrossRefRepo(
    private val dao: PlaylistTrackCrossRefDao
) {
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?> {
        return dao.get(playlistId, trackId)
    }

    suspend fun add(playlistId: PlaylistId, trackId: TrackId) {
        val currentTime = System.currentTimeMillis()
        dao.addWithAutoPosition(
            playlistId = playlistId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        if (from == to) return
        dao.changeItemPosition(playlistId, from, to)
    }

    suspend fun delete(playlistId: PlaylistId, trackId: TrackId) {
        dao.delete(playlistId, trackId)
    }
}