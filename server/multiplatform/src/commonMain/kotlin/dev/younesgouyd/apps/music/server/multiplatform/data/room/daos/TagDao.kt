package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Tag
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {
    @Query("select * from tag where id = :id")
    abstract fun get(id: TagId): Flow<Tag?>

    @Query("select * from tag where name like '%' || :nameQuery || '%'")
    abstract fun search(nameQuery: String): Flow<List<Tag>>

    @Query(
        """
        select t.*
        from tag t
        join tagtrackcrossref cr on cr.tagId = t.id
        where cr.trackId = :id
        and t.name like '%' || :name || '%'
    """
    )
    abstract fun searchTrackTags(id: TrackId, name: String): Flow<List<Tag>>

    @Query(
        """
        select t.*
        from tag t
        where t.id not in (
            select cr.tagId
            from tagtrackcrossref cr
            where cr.trackId = :id
        )
        and t.name like '%' || :name || '%'
    """
    )
    abstract fun searchTrackUnsetTags(id: TrackId, name: String): Flow<List<Tag>>

    @Query("""
        insert into tag (name, creationDatetime)
        values (:name, :creationDatetime)
    """)
    abstract suspend fun add(
        name: String,
        creationDatetime: Long
    ): Long

    @Query("delete from tag where id = :id")
    abstract suspend fun delete(id: TagId)
}