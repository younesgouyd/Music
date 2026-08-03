package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.models.Setting
import dev.younesgouyd.apps.music.common.models.rpc.SettingRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SettingRepo(
    private val backend: Backend
) {
    fun getDarkTheme(): Flow<Setting> {
        return flow {
            emit(
                backend.call(SettingRpc.GetDarkTheme).body<Setting>()
            )
        }
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        backend.call(SettingRpc.UpdateDarkTheme(theme))
    }
}