package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.Playlist
import dev.younesgouyd.apps.music.common.models.pagination.Offset
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

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return server.observe(Rpc.ToServer.Playlist.GetTrackPlaylists(id))
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
}