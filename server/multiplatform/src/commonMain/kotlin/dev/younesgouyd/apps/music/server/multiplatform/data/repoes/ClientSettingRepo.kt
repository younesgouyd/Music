package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.ClientSettingDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ClientSetting
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow

class ClientSettingRepo(
    private val dao: ClientSettingDao
) {
    object SettingName {
        const val DARK_MODE = "dark_mode"
    }

    private val logger = KotlinLogging.logger {  }

    fun get(clientId: ClientId, name: String): Flow<ClientSetting?> {
        return dao.get(clientId, name)
    }

    suspend fun set(
        clientId: ClientId,
        name: String,
        value: String
    ) {
        logger.debug { "--> ::set(clientId: $clientId, name: $name, value: $value)" }
        dao.set(clientId, name, value)
    }

    suspend fun delete(clientId: ClientId, name: String) {
        dao.delete(clientId, name)
    }
}