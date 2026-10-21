package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: PlaylistId,
    val name: String,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)