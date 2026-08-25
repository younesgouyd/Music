package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistRpc
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val server: Server
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        return server.callForResult(
            PlaylistRpc.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        return server.stream(PlaylistRpc.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return server.stream(PlaylistRpc.Search(nameQuery))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return server.stream(
            PlaylistRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return server.stream(PlaylistRpc.GetFolderPlaylists(folderId))
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return server.stream(PlaylistRpc.GetTrackPlaylists(id))
    }

    suspend fun add(
        name: String,
        folderId: FolderId?
    ): PlaylistId {
        require(name.isNotEmpty())
        return server.callForResult(
            PlaylistRpc.Add(
                name = name,
                folderId = folderId
            )
        )
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        server.call(
            PlaylistRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        server.call(
            PlaylistRpc.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }

    suspend fun delete(id: PlaylistId) {
        server.call(
            PlaylistRpc.Delete(id)
        )
    }
}