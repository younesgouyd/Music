package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.common.TrackId
import dev.younesgouyd.apps.music.common.spotifyapimodels.Track
import io.ktor.client.*

class SetTrackMetadataFromSpotifyUseCase(
    private val client: HttpClient
) {
    suspend fun execute(
        trackId: TrackId,
        spotifyApiTrack: Track
    ) {
        TODO()
    }
}