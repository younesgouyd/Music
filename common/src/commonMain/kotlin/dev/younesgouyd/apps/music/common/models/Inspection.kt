package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Inspection(
    val container: Container,
    val items: List<Item>
) {
    @Serializable
    data class Container(
        val uri: String,
        val title: String?,
        val description: String?,
        val thumbnailUrl: String?,
        val thumbnail: Base64Image?,
    )

    @Serializable
    data class Item(
        val id: Long,
        val uri: String,
        val title: String,
        val durationMilliseconds: Long,
        val artists: List<String>,
        val album: String?,
        val thumbnailUrl: String?,
        val thumbnail: Base64Image?
    )
}