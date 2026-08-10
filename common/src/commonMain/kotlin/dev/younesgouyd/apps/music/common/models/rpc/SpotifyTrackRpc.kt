package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifyTrackRpc : Rpc() {
    @Serializable
    data class GetId(
        val spotifyId: String
    ) : SpotifyTrackRpc()

    @Serializable
    data class GetAlbumTracks(
        val id: SpotifyAlbumId
    ) : SpotifyTrackRpc()
}