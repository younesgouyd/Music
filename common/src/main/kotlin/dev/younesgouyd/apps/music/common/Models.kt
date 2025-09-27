package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class Inspection(
    val items: List<Item>
) {
    @Serializable
    data class Item(
        val id: Long,
        val title: String,
        val thumbnail: String?, // url
        val artists: List<String>,
        val duration: Int?, // seconds
        val album: String?,
        val url: String
    )
}