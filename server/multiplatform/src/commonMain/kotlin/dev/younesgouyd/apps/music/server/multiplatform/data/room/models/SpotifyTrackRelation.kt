package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room.Embedded
import androidx.room.Relation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Track

data class SpotifyTrackRelation(
    @Embedded val spotifyTrack: SpotifyTrack,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotifyTrackId"
    )
    val track: Track?
)