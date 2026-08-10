package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifyArtistRpc : Rpc() {
    @Serializable
    data class Get(
        val id: SpotifyArtistId
    ) : SpotifyArtistRpc()

    @Serializable
    data class Search(
        val nameQuery: String,
        val limit: Int,
        val offset: Offset.Id<SpotifyArtistId>
    ) : SpotifyArtistRpc()

    @Serializable
    data class GetSpotifyTrackSpotifyArtists(
        val id: SpotifyTrackId
    ) : SpotifyArtistRpc()

    @Serializable
    data class GetSpotifyAlbumSpotifyArtists(
        val id: SpotifyAlbumId
    ) : SpotifyArtistRpc()
}