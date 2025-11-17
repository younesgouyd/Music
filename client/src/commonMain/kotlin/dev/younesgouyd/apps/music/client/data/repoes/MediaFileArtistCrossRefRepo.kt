package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileArtistCrossRefDao

class MediaFileArtistCrossRefRepo(
    private val dao: MediaFileArtistCrossRefDao
) {
    suspend fun add(mediaFileId: MediaFileId, artistId: ArtistId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            artistId = artistId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: MediaFileId, artistId: ArtistId) {
        dao.delete(mediaFileId = mediaFileId, artistId = artistId)
    }
}