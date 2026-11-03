package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room3.Embedded
import androidx.room3.Relation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Track

data class TrackRelation(
    @Embedded val track: Track,
    @Relation(
        parentColumns = ["importSessionItemId"],
        entityColumns = ["id"]
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumns = ["spotifyTrackId"],
        entityColumns = ["id"]
    )
    val spotifyTrack: SpotifyTrack?
)