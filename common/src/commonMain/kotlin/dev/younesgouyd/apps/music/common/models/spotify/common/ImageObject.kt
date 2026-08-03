package dev.younesgouyd.apps.music.common.models.spotify.common

import kotlinx.serialization.Serializable

@Serializable
data class ImageObject(
    val url: String,
    val height: Int? = null,
    val width: Int? = null
)