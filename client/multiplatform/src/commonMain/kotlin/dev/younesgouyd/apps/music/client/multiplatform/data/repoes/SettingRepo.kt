package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Setting
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val server: Server
) {
    fun get(name: String): Flow<Setting> {
        return server.observe(
            Rpc.ToServer.Setting.Get(name)
        )
    }

    suspend fun set(setting: Setting) {
        server.call(
            Rpc.ToServer.Setting.Set(setting)
        )
    }
}