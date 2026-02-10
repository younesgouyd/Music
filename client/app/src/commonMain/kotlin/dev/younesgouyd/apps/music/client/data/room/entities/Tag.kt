package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.coroutines.flow.Flow


@Entity
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

    @Query("select * from tag where id = :id")
    fun get(id: TagId): Flow<Tag>

    @Query("select * from tag where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<Tag>>

    @Query(
        """
        select t.*
        from tag t
        join tagtrackcrossref cr on cr.tagId = t.id
        where cr.trackId = :id
    """
    )
    fun getTrackTags(id: TrackId): Flow<List<Tag>>

    @Query(
        """
        select t.*
        from tag t
        where t.id not in (
            select cr.tagId
            from tagtrackcrossref cr
            where cr.trackId = :id
        )
    """
    )
    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>>

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