package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId

class UnsetSpotifyTrackUseCase(
    private val backend: Backend
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        TODO()
    }
}