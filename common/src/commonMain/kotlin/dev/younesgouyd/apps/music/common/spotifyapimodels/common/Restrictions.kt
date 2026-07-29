package dev.younesgouyd.apps.music.common.spotifyapimodels.common

import kotlinx.serialization.Serializable

@Serializable
data class Restrictions(
    val reason: String? = null
)