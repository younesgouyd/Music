package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val ipAddress: String,
    val certificateFingerprint: String,
    val password: String
)