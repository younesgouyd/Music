package dev.younesgouyd.apps.music.client.data.repoes

class ArtistTrackCrossRefRepo(private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ArtistTrackCrossRefDao) {
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