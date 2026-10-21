package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.DeviceType
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Client
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClientDao {
    @Query("select * from client")
    abstract fun getAll(): Flow<List<Client>>

    @Query("""
        select c.*
        from client c
        where exists (
            select 1
            from clientsession cs
            where cs.clientId = c.id
            and cs.disconnectionFlag = false
        )
    """)
    abstract fun getConnected(): Flow<List<Client>>

    @Query("""
            insert into client (name, deviceType, creationDatetime, updateDatetime)
            values (:name, :deviceType, :creationDatetime, :updateDatetime)
    """)
    abstract suspend fun add(
        name: String,
        deviceType: DeviceType?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("""
        update client
        set name = :name,
            deviceType = :deviceType,
            updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        name: String,
        deviceType: DeviceType?,
        updateDatetime: Long,
        id: ClientId
    )
}