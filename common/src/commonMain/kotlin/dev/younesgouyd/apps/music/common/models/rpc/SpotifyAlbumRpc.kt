package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifyAlbumRpc : Rpc() {
    @Serializable
    data class Get(
        val id: SpotifyAlbumId
    ) : SpotifyAlbumRpc()

    @Serializable
    data class SearchArtist(
        val id: SpotifyArtistId,
        val nameQuery: String
    ) : SpotifyAlbumRpc()
}