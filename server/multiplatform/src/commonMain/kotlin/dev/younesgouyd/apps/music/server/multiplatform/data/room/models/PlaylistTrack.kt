package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room3.Embedded
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRefId

data class PlaylistTrack(
    @Embedded val trackRelation: TrackRelation,
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId
)