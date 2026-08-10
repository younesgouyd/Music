package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAuthState(
    val clientId: String?,
    val clientSecret: String?,
    val isAuthorized: Boolean
)

