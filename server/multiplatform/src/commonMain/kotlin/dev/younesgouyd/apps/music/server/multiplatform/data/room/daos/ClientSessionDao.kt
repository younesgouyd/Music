package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ClientSessionId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ClientSession
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClientSessionDao {
    @Query("""
        select *
        from clientsession
        where clientId = :id
        order by connectionDatetime desc
        limit 50
    """)
    abstract fun getLast50(id: ClientId): Flow<List<ClientSession>>

    @Query("""
        insert into clientsession (clientId, connectionDatetime, creationDatetime, updateDatetime)
        values (:clientId, :connectionDatetime, :creationDatetime, :updateDatetime)
    """)
    abstract suspend fun add(
        clientId: ClientId,
        connectionDatetime: Long,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("""
        update clientsession
        set disconnectionDatetime = :disconnectionDatetime,
            disconnectionReason = :disconnectionReason,
            updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        disconnectionDatetime: Long,
        disconnectionReason: ClientSession.DisconnectionReason,
        updateDatetime: Long,
        id: ClientSessionId
    )

    @Query("""
        update clientsession
        set disconnectionReason = :disconnectionReason,
            updateDatetime = :updateDatetime
        where disconnectionReason is null
    """)
    abstract suspend fun updateAll(
        disconnectionReason: ClientSession.DisconnectionReason,
        updateDatetime: Long
    )
}