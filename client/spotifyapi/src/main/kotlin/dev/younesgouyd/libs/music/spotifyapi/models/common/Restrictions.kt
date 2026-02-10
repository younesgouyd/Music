package dev.younesgouyd.libs.music.spotifyapi.models.common

import kotlinx.serialization.Serializable

@Serializable
data class Restrictions(
    val reason: String? = null
)