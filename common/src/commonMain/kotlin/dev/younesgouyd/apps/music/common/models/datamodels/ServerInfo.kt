package dev.younesgouyd.apps.music.common.models.datamodels

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val certificateFingerprint: String,
    val password: String
)