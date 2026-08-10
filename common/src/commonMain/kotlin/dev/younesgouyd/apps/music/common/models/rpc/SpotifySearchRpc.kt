package dev.younesgouyd.apps.music.common.models.rpc

import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifySearchRpc : Rpc() {
    @Serializable
    data class Search(
        val track: String,
        val artist: String?,
        val album: String?,
        val year: String?
    ) : SpotifySearchRpc()
}