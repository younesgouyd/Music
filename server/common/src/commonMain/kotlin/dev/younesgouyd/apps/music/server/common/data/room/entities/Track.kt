package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.common.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importSessionItemId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SpotifyTrack::class,
            parentColumns = ["id"],
            childColumns = ["spotifyTrackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT // TODO: if a track is deleted, also delete its SpotifyTrack if unused elsewhere, and clear its imports if unused
        )
    ],
    indices = [
        Index(value = ["importSessionItemId"], unique = true),
        Index(value = ["spotifyTrackId"], unique = true),
        Index(value = ["folderId"])
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: TrackId,
    val importSessionItemId: ImportSessionItemId,
    val spotifyTrackId: SpotifyTrackId?,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

data class TrackRelation(
    @Embedded val track: Track,
    @Relation(
        parentColumn = "importSessionItemId",
        entityColumn = "id"
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumn = "spotifyTrackId",
        entityColumn = "id"
    )
    val spotifyTrack: SpotifyTrack?
)

data class PlaylistTrack(
    @Embedded val track: Track,
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId,
    @Relation(
        parentColumn = "importSessionItemId",
        entityColumn = "id"
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumn = "spotifyTrackId",
        entityColumn = "id"
    )
    val spotifyTrack: SpotifyTrack?,
    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val playlistCrossRef: PlaylistTrackCrossRef?
)