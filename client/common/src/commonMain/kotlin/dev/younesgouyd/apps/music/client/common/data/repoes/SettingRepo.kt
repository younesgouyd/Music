package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.models.Setting
import dev.younesgouyd.apps.music.common.models.rpc.SettingRpc
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val backend: Backend
) {
    fun getDarkTheme(): Flow<Setting> {
        return backend.stream(SettingRpc.GetDarkTheme)
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        backend.call(SettingRpc.UpdateDarkTheme(theme))
    }
}