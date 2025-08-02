package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Playlist_track_cross_ref
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.PlaylistTrackCrossRefQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistTrackCrossRefRepo(private val queries: PlaylistTrackCrossRefQueries) {
    suspend fun add(playlistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
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

    suspend fun getStatic(playlistId: Long, trackId: Long): Playlist_track_cross_ref? {
        return withContext(Dispatchers.IO) {
            queries.get(playlistId, trackId).executeAsOneOrNull()
        }
    }
}