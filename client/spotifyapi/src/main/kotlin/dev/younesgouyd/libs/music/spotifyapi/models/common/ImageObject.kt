package dev.younesgouyd.libs.music.spotifyapi.models.common

import kotlinx.serialization.Serializable

@Serializable
data class ImageObject(
    val url: String,
    val height: Int? = null,
    val width: Int? = null
)