package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room.Embedded
import androidx.room.Relation
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Track

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