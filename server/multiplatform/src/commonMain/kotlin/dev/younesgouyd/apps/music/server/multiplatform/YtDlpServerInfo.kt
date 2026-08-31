package dev.younesgouyd.apps.music.server.multiplatform

data class YtDlpServerInfo(
    val ipAddress: String,
    val serverFingerprint: String,
    val password: String
) {
    companion object {
        val Empty get() = YtDlpServerInfo("", "", "")
    }
}