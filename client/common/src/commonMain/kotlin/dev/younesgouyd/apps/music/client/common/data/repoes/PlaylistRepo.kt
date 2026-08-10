package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistRpc
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val backend: Backend
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        return backend.callForResult(
            PlaylistRpc.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        return backend.stream(PlaylistRpc.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return backend.stream(PlaylistRpc.Search(nameQuery))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return backend.stream(
            PlaylistRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return backend.stream(PlaylistRpc.GetFolderPlaylists(folderId))
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return backend.stream(PlaylistRpc.GetTrackPlaylists(id))
    }

    suspend fun add(
        name: String,
        folderId: FolderId?
    ): PlaylistId {
        require(name.isNotEmpty())
        return backend.callForResult(
            PlaylistRpc.Add(
                name = name,
                folderId = folderId
            )
        )
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        backend.call(
            PlaylistRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        backend.call(
            PlaylistRpc.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }

    suspend fun delete(id: PlaylistId) {
        backend.call(
            PlaylistRpc.Delete(id)
        )
    }
}