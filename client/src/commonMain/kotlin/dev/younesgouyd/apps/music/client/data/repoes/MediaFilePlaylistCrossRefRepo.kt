package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFilePlaylistCrossRef
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFilePlaylistCrossRefDao
import kotlinx.coroutines.flow.Flow

class MediaFilePlaylistCrossRefRepo(
    private val dao: MediaFilePlaylistCrossRefDao
) {
    fun getAll(): Flow<List<MediaFilePlaylistCrossRef>> {
        return dao.getAll()
    }

    suspend fun add(mediaFileId: MediaFileId, playlistId: PlaylistId) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            playlistId = playlistId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: MediaFileId, playlistId: PlaylistId) {
        dao.delete(mediaFileId = mediaFileId, playlistId = playlistId)
    }
}