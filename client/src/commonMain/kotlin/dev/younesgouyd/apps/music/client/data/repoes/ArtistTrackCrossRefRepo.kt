package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.ArtistTrackCrossRefDao

class ArtistTrackCrossRefRepo(private val dao: ArtistTrackCrossRefDao) {
    suspend fun add(artistId: Long, trackId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            artistId = artistId,
            trackId = trackId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun delete(artistId: Long, trackId: Long) {
        dao.delete(artistId, trackId)
    }
}