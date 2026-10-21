package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ClientSessionId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.ClientSessionDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ClientSession
import kotlinx.coroutines.flow.Flow

class ClientSessionRepo(
    private val dao: ClientSessionDao
) {
    fun getLast50(id: ClientId): Flow<List<ClientSession>> {
        return dao.getLast50(id)
    }

    suspend fun add(
        clientId: ClientId,
        connectionDatetime: Long
    ): ClientSessionId {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            clientId = clientId,
            connectionDatetime = connectionDatetime,
            disconnectionFlag = false,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return ClientSessionId(id)
    }

    suspend fun update(
        id: ClientSessionId,
        disconnectionDatetime: Long
    ) {
        dao.update(
            disconnectionDatetime = disconnectionDatetime,
            disconnectionFlag = true,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun updateDisconnectionFlag() {
        dao.update(
            disconnectionFlag = true,
            updateDatetime = System.currentTimeMillis()
        )
    }
}