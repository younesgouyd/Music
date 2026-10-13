package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.DeviceType
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.ClientDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Client
import kotlinx.coroutines.flow.Flow

class ClientRepo(
    private val dao: ClientDao
) {
    fun getAll(): Flow<List<Client>> {
        return dao.getAll()
    }

    fun getConnected(): Flow<List<Client>> {
        return dao.getConnected()
    }

    suspend fun add(
        name: String,
        deviceType: DeviceType?
    ): ClientId {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            deviceType = deviceType,
            name = name,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return ClientId(id)
    }

    suspend fun update(id: ClientId, name: String, deviceType: DeviceType?) {
        dao.update(name, deviceType, System.currentTimeMillis(), id)
    }
}