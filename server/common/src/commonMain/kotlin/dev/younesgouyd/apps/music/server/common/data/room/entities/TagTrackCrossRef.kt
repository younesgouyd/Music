package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TagTrackCrossRefId
import dev.younesgouyd.apps.music.common.models.TrackId

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