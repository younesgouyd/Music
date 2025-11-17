package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.ArtistTrackCrossRefDao

class ArtistTrackCrossRefRepo(
    private val dao: ArtistTrackCrossRefDao
) {
    suspend fun add(artistId: ArtistId, trackId: TrackId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            artistId = artistId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(artistId: ArtistId, trackId: TrackId) {
        dao.delete(artistId = artistId, trackId = trackId)
    }
}