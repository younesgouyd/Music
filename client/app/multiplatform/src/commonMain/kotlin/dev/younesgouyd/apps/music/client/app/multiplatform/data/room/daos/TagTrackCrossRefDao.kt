package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TagId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId

@Dao
abstract class TagTrackCrossRefDao {
    @Query("""
        insert into tagtrackcrossref (tagId, trackId, creationDatetime)
        values (:tagId, :trackId, :creationDatetime)
    """)
    abstract suspend fun add(tagId: TagId, trackId: TrackId, creationDatetime: Long)

    @Query("delete from tagtrackcrossref where tagId = :tagId and trackId = :trackId")
    abstract suspend fun delete(tagId: TagId, trackId: TrackId)
}