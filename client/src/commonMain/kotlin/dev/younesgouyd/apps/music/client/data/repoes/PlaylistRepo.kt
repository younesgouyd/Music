package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.Playlist
import dev.younesgouyd.apps.music.client.data.room.entities.PlaylistDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(private val dao: PlaylistDao) {
    fun getAll(): Flow<List<Playlist>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Playlist> {
        return dao.get(id)
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun searchFolder(
        folderId: Long?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun getImportSessionPlaylist(importSessionId: Long): Flow<Playlist?> {
        return dao.getImportSessionPlaylist(importSessionId)
    }

    fun getFolderPlaylists(folderId: Long?): Flow<List<Playlist>> {
        return dao.getFolderPlaylists(folderId)
    }

    suspend fun add(
        name: String,
        folderId: Long?,
        image: ByteArray?,
        importSessionId: Long?,
        importUri: String?
    ): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            folderId = folderId,
            image = image,
            importSessionId = importSessionId,
            importUri = importUri,
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