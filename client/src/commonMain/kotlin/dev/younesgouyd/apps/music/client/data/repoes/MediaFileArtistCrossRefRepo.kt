package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileArtistCrossRefDao

class MediaFileArtistCrossRefRepo(
    private val dao: MediaFileArtistCrossRefDao
) {
    suspend fun add(mediaFileId: Long, artistId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            artistId = artistId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: Long, artistId: Long) {
        dao.delete(mediaFileId = mediaFileId, artistId = artistId)
    }
}