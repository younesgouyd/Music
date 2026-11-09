package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.*

data class SpotifyTrackRelation(
    @Embedded val spotifyTrack: SpotifyTrack,

    @Relation(
        parentColumns = ["spotifyAlbumId"],
        entityColumns = ["id"]
    )
    val spotifyAlbum: SpotifyAlbum,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy = Junction(
            value = SpotifyArtistSpotifyTrackCrossRef::class,
            parentColumns = ["spotifyTrackId"],
            entityColumns = ["spotifyArtistId"]
        )
    )
    val spotifyArtists: List<SpotifyArtist>,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["spotifyTrackId"]
    )
    val track: Track?,
)