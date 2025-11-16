package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.MediaFilePlaylistCrossRefDao

class MediaFilePlaylistCrossRefRepo(
    private val dao: MediaFilePlaylistCrossRefDao
) {
    suspend fun add(mediaFileId: Long, playlistId: Long) {
        val currentTime = System.currentTimeMillis()
        dao.add(
            mediaFileId = mediaFileId,
            playlistId = playlistId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }


    suspend fun delete(mediaFileId: Long, playlistId: Long) {
        dao.delete(mediaFileId = mediaFileId, playlistId = playlistId)
    }
}