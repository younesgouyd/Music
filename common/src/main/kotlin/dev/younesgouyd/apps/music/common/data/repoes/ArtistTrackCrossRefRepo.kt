package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.sqldelight.queries.ArtistTrackCrossRefQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class ArtistTrackCrossRefRepo(private val queries: ArtistTrackCrossRefQueries) {
    suspend fun add(artistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            val currentTime = Instant.now().toEpochMilli()
            queries.add(
                artist_id = artistId,
                track_id = trackId,
                creation_datetime = currentTime,
                update_datetime = currentTime
            )
        }
    }

    suspend fun delete(artistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(artistId, trackId)
        }
    }
}