package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.SettingDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Setting
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val dao: SettingDao
) {
    object SettingName {
        const val PASSWORD = "password"
        const val YT_DLP_SERVER_INFO = "ytdlp_server_info"
    }

    fun get(name: String): Flow<Setting?> {
        return dao.get(name)
    }

    suspend fun set(
        name: String,
        value: String
    ) {
        dao.set(name, value)
    }

    suspend fun delete(name: String) {
        dao.delete(name)
    }
}