package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
data class UnsetSpotifyTrackRpc(
    val trackId: TrackId,
    val spotifyTrackId: SpotifyTrackId,
    val spotifyAlbumId: SpotifyAlbumId
) : Rpc()
