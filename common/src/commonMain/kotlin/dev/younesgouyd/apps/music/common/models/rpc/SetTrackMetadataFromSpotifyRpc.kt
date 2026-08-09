package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.spotify.Track
import kotlinx.serialization.Serializable

@Serializable
data class SetTrackMetadataFromSpotifyRpc(
    val trackId: TrackId,
    val spotifyApiTrack: Track
) : Rpc()
