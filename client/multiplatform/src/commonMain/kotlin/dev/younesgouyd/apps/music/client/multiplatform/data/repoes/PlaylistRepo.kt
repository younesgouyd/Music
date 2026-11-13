package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.Playlist
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val server: Server
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        return server.get(
            Rpc.ToServer.Playlist.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        return server.observe(Rpc.ToServer.Playlist.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return server.observe(Rpc.ToServer.Playlist.Search(nameQuery))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return server.observe(
            Rpc.ToServer.Playlist.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return server.observe(Rpc.ToServer.Playlist.GetFolderPlaylists(folderId))
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return server.observe(Rpc.ToServer.Playlist.GetTrackPlaylists(id))
    }

    suspend fun add(
        name: String,
        folderId: FolderId?
    ): PlaylistId {
        require(name.isNotEmpty())
        return server.get(
            Rpc.ToServer.Playlist.Add(
                name = name,
                folderId = folderId
            )
        )
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        server.call(
            Rpc.ToServer.Playlist.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId?) {
        server.call(
            Rpc.ToServer.Playlist.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }

    suspend fun delete(id: PlaylistId) {
        server.call(
            Rpc.ToServer.Playlist.Delete(id)
        )
    }
}