package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Setting
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SettingDao {
    @Query("select * from setting where name = :name")
    abstract fun get(name: String): Flow<Setting?>

    @Transaction
    open suspend fun set(
        name: String,
        value: String
    ) {
        if (exists(name) != null) {
            update(name, value, System.currentTimeMillis())
        } else {
            add(name, value, System.currentTimeMillis())
        }
    }

    @Query("select 1 from setting where name = :name")
    abstract suspend fun exists(name: String): Int?

    @Query("""
        insert into setting (name, value, creationDatetime, updateDatetime)
        values (:name, :value, :datetime, :datetime)
    """)
    abstract suspend fun add(
        name: String,
        value: String,
        datetime: Long
    )

    @Query("""
        update setting 
        set value = :value,
            updateDatetime = :datetime
        where name = :name
    """)
    abstract suspend fun update(
        name: String,
        value: String,
        datetime: Long
    )

    @Query("delete from setting where name = :name")
    abstract suspend fun delete(name: String)
}