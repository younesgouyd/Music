package dev.younesgouyd.apps.music.app.data.repoes

import dev.younesgouyd.apps.music.app.data.sqldelight.queries.PlaylistTrackCrossRefQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class PlaylistTrackCrossRefRepo(private val queries: PlaylistTrackCrossRefQueries) {
    suspend fun add(playlistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            val currentTime = Instant.now().toEpochMilli()
            queries.add(
                playlist_id = playlistId,
                track_id = trackId,
                creation_datetime = currentTime,
                update_datetime = currentTime
            )
        }
    }

    suspend fun delete(playlistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(playlistId, trackId)
        }
    }
}