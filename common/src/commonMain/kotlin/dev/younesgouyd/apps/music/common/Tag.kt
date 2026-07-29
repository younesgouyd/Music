package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)