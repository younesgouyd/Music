package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
sealed class PlaylistTrackCrossRefRpc : Rpc() {
    @Serializable
    data class Get(
        val playlistId: PlaylistId,
        val trackId: TrackId
    ) : PlaylistTrackCrossRefRpc()

    @Serializable
    data class Add(
        val playlistId: PlaylistId,
        val trackId: TrackId
    ) : PlaylistTrackCrossRefRpc()

    @Serializable
    data class ChangeItemPosition(
        val playlistId: PlaylistId,
        val from: Int,
        val to: Int
    ) : PlaylistTrackCrossRefRpc()

    @Serializable
    data class Delete(
        val playlistId: PlaylistId,
        val trackId: TrackId
    ) : PlaylistTrackCrossRefRpc()
}