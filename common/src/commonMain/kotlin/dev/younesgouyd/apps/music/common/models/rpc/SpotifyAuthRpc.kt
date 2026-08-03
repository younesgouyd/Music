package dev.younesgouyd.apps.music.common.models.rpc

import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifyAuthRpc : Rpc() {
    @Serializable
    data object GetAuthState : SpotifyAuthRpc()

    @Serializable
    data class Authorize(
        val clientId: String,
        val clientSecret: String
    ) : SpotifyAuthRpc()

    @Serializable
    data object Deauthorize : SpotifyAuthRpc()
}