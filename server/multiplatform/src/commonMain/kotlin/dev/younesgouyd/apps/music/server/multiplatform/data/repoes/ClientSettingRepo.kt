package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.datamodels.Setting
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.ClientSettingDao
import dev.younesgouyd.apps.music.server.multiplatform.data.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClientSettingRepo(
    private val dao: ClientSettingDao
) {
    fun get(clientId: ClientId, name: String): Flow<Setting> {
        return dao.get(clientId, name).map { it.toModel()}
    }

    suspend fun set(
        clientId: ClientId,
        setting: Setting
    ) {
        dao.set(clientId, setting.name, setting.valueSerialized)
    }
}