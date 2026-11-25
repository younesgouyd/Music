package dev.younesgouyd.apps.music.common.models.datamodels.home

import dev.younesgouyd.apps.music.common.models.HomeItemType
import kotlinx.serialization.Serializable

@Serializable
data class HomeFeedStub(
    val itemType: HomeItemType,
    val itemId: Long,
    val creationDatetime: Long
)