package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.PlaylistId
import dev.younesgouyd.apps.music.common.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.common.TrackId
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class PlaylistTrackCrossRefRepo(
    private val client: HttpClient
) {
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?> {
        TODO()
    }

    suspend fun add(playlistId: PlaylistId, trackId: TrackId) {
        TODO()
//        val currentTime = System.currentTimeMillis()
//        dao.addWithAutoPosition(
//            playlistId = playlistId,
//            trackId = trackId,
//            creationDatetime = currentTime,
//            updateDatetime = currentTime
//        )
    }

    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        TODO()
//        if (from == to) return
//        dao.changeItemPosition(playlistId, from, to)
    }

    suspend fun delete(playlistId: PlaylistId, trackId: TrackId) {
//        dao.delete(playlistId, trackId)
        TODO()
    }
}