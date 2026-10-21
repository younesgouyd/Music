package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.TagId
import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)