package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
sealed class PlaylistRpc : Rpc() {
    @Serializable
    data class GetAll(
        val limit: Int,
        val offset: Offset.Id<PlaylistId>
    ) : PlaylistRpc()

    @Serializable
    data class Get(
        val id: PlaylistId
    ) : PlaylistRpc()

    @Serializable
    data class Search(
        val nameQuery: String
    ) : PlaylistRpc()

    @Serializable
    data class SearchFolder(
        val folderId: FolderId?,
        val nameQuery: String
    ) : PlaylistRpc()

    @Serializable
    data class GetFolderPlaylists(
        val folderId: FolderId?
    ) : PlaylistRpc()

    @Serializable
    data class GetTrackPlaylists(
        val id: TrackId
    ) : PlaylistRpc()

    @Serializable
    data class Add(
        val name: String,
        val folderId: FolderId?
    ) : PlaylistRpc()

    @Serializable
    data class UpdateName(
        val id: PlaylistId,
        val name: String
    ) : PlaylistRpc()

    @Serializable
    data class UpdateFolderId(
        val id: PlaylistId,
        val folderId: FolderId
    ) : PlaylistRpc()

    @Serializable
    data class Delete(
        val id: PlaylistId
    ) : PlaylistRpc()
}