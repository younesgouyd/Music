package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Server
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
        server.call(SetTrackMetadataFromSpotifyRpc(trackId, spotifyApiTrack))
    }
}