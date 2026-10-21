package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: TrackId,
    val importSessionItemId: ImportSessionItemId,
    val spotifyTrackId: SpotifyTrackId?,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)
