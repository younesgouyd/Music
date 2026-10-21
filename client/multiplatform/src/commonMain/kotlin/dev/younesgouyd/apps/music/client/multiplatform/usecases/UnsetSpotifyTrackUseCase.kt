package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId

class UnsetSpotifyTrackUseCase(
    private val server: Server
) {
    suspend fun execute(
        trackId: TrackId,
        spotifyTrackId: SpotifyTrackId,
        spotifyAlbumId: SpotifyAlbumId
    ) {
        server.call(
            Rpc.ToServer.UnsetSpotifyTrack(trackId, spotifyTrackId, spotifyAlbumId)
        )
    }
}