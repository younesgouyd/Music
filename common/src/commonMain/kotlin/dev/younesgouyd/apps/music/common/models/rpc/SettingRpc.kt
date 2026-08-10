package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import kotlinx.serialization.Serializable

@Serializable
sealed class SettingRpc : Rpc() {
    @Serializable
    data object GetDarkTheme : SettingRpc()

    @Serializable
    data class UpdateDarkTheme(
        val theme: DarkThemeOptions
    ) : SettingRpc()
}