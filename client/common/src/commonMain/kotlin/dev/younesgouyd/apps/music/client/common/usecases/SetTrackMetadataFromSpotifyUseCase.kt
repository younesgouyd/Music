package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.SetTrackMetadataFromSpotifyRpc
import dev.younesgouyd.apps.music.common.models.spotify.Track

class SetTrackMetadataFromSpotifyUseCase(
    private val backend: Backend
) {
    suspend fun execute(
        trackId: TrackId,
        spotifyApiTrack: Track
    ) {
        backend.call(SetTrackMetadataFromSpotifyRpc(trackId, spotifyApiTrack))
    }
}