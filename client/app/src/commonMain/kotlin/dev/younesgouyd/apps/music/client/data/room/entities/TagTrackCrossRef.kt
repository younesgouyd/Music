package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TagTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId

@Entity(
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
    ],
    indices = [
        Index(value = ["tagId", "trackId"], unique = true),
        Index(value = ["tagId"]),
        Index(value = ["trackId"])
    ]
)
data class TagTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: TagTrackCrossRefId,
    val tagId: TagId,
    val trackId: TrackId,
    val creationDatetime: Long
)

@Dao
interface TagTrackCrossRefDao {
    @Query("""
        insert into tagtrackcrossref (tagId, trackId, creationDatetime)
        values (:tagId, :trackId, :creationDatetime)
    """)
    suspend fun add(tagId: TagId, trackId: TrackId, creationDatetime: Long)

    @Query("delete from tagtrackcrossref where tagId = :tagId and trackId = :trackId")
    suspend fun delete(tagId: TagId, trackId: TrackId)
}