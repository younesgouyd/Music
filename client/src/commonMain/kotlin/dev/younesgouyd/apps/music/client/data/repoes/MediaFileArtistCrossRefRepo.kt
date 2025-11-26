package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileArtistCrossRef
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileArtistCrossRefDao
import kotlinx.coroutines.flow.Flow

class MediaFileArtistCrossRefRepo(
    private val dao: MediaFileArtistCrossRefDao
) {
    fun getAll(): Flow<List<MediaFileArtistCrossRef>> {
        return dao.getAll()
    }

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