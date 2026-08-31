package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistRpc
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val server: Server
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        return server.call(
            PlaylistRpc.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        return server.observe(PlaylistRpc.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return server.observe(PlaylistRpc.Search(nameQuery))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return server.observe(
            PlaylistRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return server.observe(PlaylistRpc.GetFolderPlaylists(folderId))
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return server.observe(PlaylistRpc.GetTrackPlaylists(id))
    }

    suspend fun add(
        name: String,
        folderId: FolderId?
    ): PlaylistId {
        require(name.isNotEmpty())
        return server.call(
            PlaylistRpc.Add(
                name = name,
                folderId = folderId
            )
        )
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        server.call<Unit>(
            PlaylistRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        server.call<Unit>(
            PlaylistRpc.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }

    suspend fun delete(id: PlaylistId) {
        server.call<Unit>(
            PlaylistRpc.Delete(id)
        )
    }
}