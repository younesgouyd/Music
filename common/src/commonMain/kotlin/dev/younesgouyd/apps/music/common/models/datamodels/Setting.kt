package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.ClientSettingId
import kotlinx.serialization.Serializable

@Serializable
data class Setting(
    val id: ClientSettingId,
    val name: String,
    val value: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)