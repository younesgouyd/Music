package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: PlaylistId,
    val name: String,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)