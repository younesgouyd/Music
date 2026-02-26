package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyArtistSpotifyTrackCrossRefId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyTrackId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SpotifyArtist::class,
            parentColumns = ["id"],
            childColumns = ["spotifyArtistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SpotifyTrack::class,
            parentColumns = ["id"],
            childColumns = ["spotifyTrackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["spotifyArtistId", "spotifyTrackId"], unique = true),
        Index(value = ["spotifyArtistId"]),
        Index(value = ["spotifyTrackId"])
    ]
)
data class SpotifyArtistSpotifyTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: SpotifyArtistSpotifyTrackCrossRefId,
    val spotifyArtistId: SpotifyArtistId,
    val spotifyTrackId: SpotifyTrackId,
    val creationDatetime: Long
)