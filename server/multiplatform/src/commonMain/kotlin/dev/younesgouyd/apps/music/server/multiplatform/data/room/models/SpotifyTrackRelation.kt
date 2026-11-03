package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room3.Embedded
import androidx.room3.Relation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Track

data class SpotifyTrackRelation(
    @Embedded val spotifyTrack: SpotifyTrack,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["spotifyTrackId"]
    )
    val track: Track?
)