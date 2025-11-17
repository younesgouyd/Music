package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.room.entities.Playlist
import dev.younesgouyd.apps.music.client.data.room.entities.PlaylistDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val dao: PlaylistDao
) {
    fun getAll(): Flow<List<Playlist>> {
        return dao.getAll()
    }

    fun get(id: PlaylistId): Flow<Playlist> {
        return dao.get(id)
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun getImportSessionPlaylist(importSessionId: ImportSessionId): Flow<Playlist?> {
        return dao.getImportSessionPlaylist(importSessionId)
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return dao.getFolderPlaylists(folderId)
    }

    suspend fun add(
        name: String,
        folderId: FolderId?,
        importSessionId: ImportSessionId?,
        importUri: String?
    ): PlaylistId {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name,
            folderId = folderId,
            importSessionId = importSessionId,
            importUri = importUri,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return PlaylistId(id)
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: PlaylistId) {
        dao.delete(id)
    }
}