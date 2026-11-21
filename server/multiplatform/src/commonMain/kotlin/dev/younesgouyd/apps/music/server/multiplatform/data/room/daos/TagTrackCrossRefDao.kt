package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId

@Dao
abstract class TagTrackCrossRefDao {
    @Query("delete from tagtrackcrossref where tagId = :tagId and trackId = :trackId")
    abstract suspend fun delete(tagId: TagId, trackId: TrackId)
}