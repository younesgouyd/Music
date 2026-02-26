package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TagId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TagTrackCrossRefId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId

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