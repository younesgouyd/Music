package dev.younesgouyd.apps.music.server.multiplatform.data.room.models

import androidx.room3.Embedded
import androidx.room3.Relation
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Track

data class PlaylistTrack(
    @Embedded val track: Track,
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId,
    @Relation(
        parentColumns = ["importSessionItemId"],
        entityColumns = ["id"]
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumns = ["spotifyTrackId"],
        entityColumns = ["id"]
    )
    val spotifyTrack: SpotifyTrack?,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["trackId"]
    )
    val playlistCrossRef: PlaylistTrackCrossRef?
)