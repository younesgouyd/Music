package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.SetTrackMetadataFromSpotifyRpc
import dev.younesgouyd.apps.music.common.models.spotify.Track

class SetTrackMetadataFromSpotifyUseCase(
    private val server: Server
) {
    suspend fun execute(
        trackId: TrackId,
        spotifyApiTrack: Track
    ) {
        server.call<Unit>(SetTrackMetadataFromSpotifyRpc(trackId, spotifyApiTrack))
    }
}