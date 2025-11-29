package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TagTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["tagId", "trackId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class TagTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: TagTrackCrossRefId,
    val tagId: TagId,
    val trackId: TrackId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface TagTrackCrossRefDao {
    @Query("""
        insert into tagtrackcrossref (tagId, trackId, creationDatetime, updateDatetime)
        values (:tagId, :trackId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(tagId: TagId, trackId: TrackId, creationDatetime: Long, updateDatetime: Long)

    @Query("delete from tagtrackcrossref where tagId = :tagId and trackId = :trackId")
    suspend fun delete(tagId: TagId, trackId: TrackId)
}