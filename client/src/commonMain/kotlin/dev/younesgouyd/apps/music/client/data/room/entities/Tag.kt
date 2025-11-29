package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable


@Entity
@Serializable
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: TagId,
    val name: String,
    val description: String?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface TagDao {
    @Query("select * from tag")
    fun getAll(): Flow<List<Tag>>

    @Query("select * from tag where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<Tag>>

    @Query("""
        select tg.*
        from tag tg
        join tagtrackcrossref cr on cr.tagId = tg.id
        where cr.trackId = :trackId
    """)
    fun getTrackTags(trackId: TrackId): Flow<List<Tag>>

    @Query("""
        insert into tag (name, description, creationDatetime, updateDatetime)
        values (:name, :description, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        name: String,
        description: String?,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("""
        update tag
        set description = :description,
            updateDatetime = :updateDatetime
        where id = :id
    """)
    suspend fun updateDescription(description: String?, updateDatetime: Long, id: TagId)

    @Query("delete from tag where id = :id")
    suspend fun delete(id: TagId)
}