package dev.younesgouyd.apps.music.common.models.pagination

import kotlinx.serialization.Serializable

@Serializable
data class Page<Item, OffsetType: Offset>(
    val nextOffset: OffsetType?,
    val items: List<Item>
)