package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.SpotifyArtistId
import dev.younesgouyd.apps.music.common.SpotifyArtistSpotifyAlbumCrossRefId

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
            entity = SpotifyAlbum::class,
            parentColumns = ["id"],
            childColumns = ["spotifyAlbumId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["spotifyArtistId", "spotifyAlbumId"], unique = true),
        Index(value = ["spotifyArtistId"]),
        Index(value = ["spotifyAlbumId"])
    ]
)
data class SpotifyArtistSpotifyAlbumCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: SpotifyArtistSpotifyAlbumCrossRefId,
    val spotifyArtistId: SpotifyArtistId,
    val spotifyAlbumId: SpotifyAlbumId,
    val creationDatetime: Long
)