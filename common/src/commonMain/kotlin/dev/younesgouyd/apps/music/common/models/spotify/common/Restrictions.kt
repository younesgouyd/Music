package dev.younesgouyd.apps.music.common.models.spotify.common

import kotlinx.serialization.Serializable

@Serializable
data class Restrictions(
    val reason: String? = null
)