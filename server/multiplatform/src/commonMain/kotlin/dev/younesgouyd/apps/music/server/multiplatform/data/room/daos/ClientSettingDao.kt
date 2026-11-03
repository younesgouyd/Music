package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ClientSetting
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClientSettingDao {
    private val logger = KotlinLogging.logger {  }

    @Query("select * from clientsetting where clientId = :clientId and name = :name")
    abstract fun get(clientId: ClientId, name: String): Flow<ClientSetting?>

    @Transaction
    open suspend fun set(
        clientId: ClientId,
        name: String,
        value: String
    ) {
        logger.debug { "--> ::set(clientId: $clientId, name: $name, value: $value)" }
        if (exists(clientId, name) != null) {
            logger.debug { "::set | updating (clientId: $clientId, name: $name, value: $value)" }
            update(clientId = clientId, name = name, value = value, datetime = System.currentTimeMillis())
        } else {
            logger.debug { "::set | adding (clientId: $clientId, name: $name, value: $value)" }
            add(clientId = clientId, name = name, value = value, datetime = System.currentTimeMillis())
        }
    }

    @Query("delete from clientsetting where clientId = :clientId and name = :name")
    abstract suspend fun delete(clientId: ClientId, name: String)

    @Query("select 1 from clientsetting where clientId = :clientId and name = :name")
    abstract suspend fun exists(clientId: ClientId, name: String): Int?

    @Query("""
        insert into clientsetting (clientId, name, value, creationDatetime, updateDatetime)
        values (:clientId, :name, :value, :datetime, :datetime)
    """)
    abstract suspend fun add(
        clientId: ClientId,
        name: String,
        value: String,
        datetime: Long
    )

    @Query("""
        update clientsetting 
        set value = :value,
            updateDatetime = :datetime
        where clientId = :clientId 
        and name = :name
    """)
    abstract suspend fun update(
        clientId: ClientId,
        name: String,
        value: String,
        datetime: Long
    )
}