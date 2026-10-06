package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class YtDlpServerInfo(
    val ipAddress: String,
    val serverFingerprint: String,
    val password: String
) {
    companion object {
        val Empty get() = YtDlpServerInfo("", "", "")
    }
}