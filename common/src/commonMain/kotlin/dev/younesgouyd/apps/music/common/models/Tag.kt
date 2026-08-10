package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)