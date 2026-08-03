package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Setting(
    val id: SettingId,
    val name: String,
    val value: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)