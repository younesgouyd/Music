package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.common.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.SpotifyTrackId
import dev.younesgouyd.apps.music.common.TrackId
import io.ktor.client.*

class UnsetSpotifyTrackUseCase(
    private val client: HttpClient
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        TODO()
    }
}