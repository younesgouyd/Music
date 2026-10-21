package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Setting
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val server: Server
) {
    fun getDarkTheme(): Flow<Setting?> {
        return server.observe(Rpc.ToServer.Setting.GetDarkTheme)
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        server.call(Rpc.ToServer.Setting.UpdateDarkTheme(theme))
    }
}