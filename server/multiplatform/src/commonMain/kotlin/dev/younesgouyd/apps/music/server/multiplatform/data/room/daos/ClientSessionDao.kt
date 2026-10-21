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
        insert into clientsession (clientId, connectionDatetime, disconnectionFlag, creationDatetime, updateDatetime)
        values (:clientId, :connectionDatetime, :disconnectionFlag, :creationDatetime, :updateDatetime)
    """)
    abstract suspend fun add(
        clientId: ClientId,
        connectionDatetime: Long,
        disconnectionFlag: Boolean,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("""
        update clientsession
        set disconnectionDatetime = :disconnectionDatetime,
            disconnectionFlag = :disconnectionFlag,
            updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        disconnectionDatetime: Long,
        disconnectionFlag: Boolean,
        updateDatetime: Long,
        id: ClientSessionId
    )

    @Query("""
        update clientsession
        set disconnectionFlag = :disconnectionFlag,
            updateDatetime = :updateDatetime
        where disconnectionDatetime is null
    """)
    abstract suspend fun update(
        disconnectionFlag: Boolean,
        updateDatetime: Long
    )
}