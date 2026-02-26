package dev.younesgouyd.libs.music.client.spotifyapi.models.common

import kotlinx.serialization.Serializable

@Serializable
data class Restrictions(
    val reason: String? = null
)