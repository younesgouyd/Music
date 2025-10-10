package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.Playlist
import dev.younesgouyd.apps.music.common.data.room.entities.PlaylistDao
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(private val dao: PlaylistDao) {
    fun getAll(): Flow<List<Playlist>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Playlist> {
        return dao.get(id)
    }

    fun getFolderPlaylists(folderId: Long?): Flow<List<Playlist>> {
        return dao.getFolderPlaylists(folderId)
    }

    fun getTrackPlaylists(trackId: Long): Flow<List<Playlist>> {
        return dao.getTrackPlaylists(trackId)
    }

    suspend fun add(name: String, folderId: Long?, image: ByteArray?): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            folderId = folderId,
            image = image,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateFolderId(id: Long, folderId: Long) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }

    suspend fun updateImage(id: Long, image: ByteArray?) {
        dao.updateImage(image, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}