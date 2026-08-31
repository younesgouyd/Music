package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.models.Setting
import dev.younesgouyd.apps.music.common.models.rpc.SettingRpc
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val server: Server
) {
    fun getDarkTheme(): Flow<Setting> {
        return server.observe(SettingRpc.GetDarkTheme)
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        server.call<Unit>(SettingRpc.UpdateDarkTheme(theme))
    }
}