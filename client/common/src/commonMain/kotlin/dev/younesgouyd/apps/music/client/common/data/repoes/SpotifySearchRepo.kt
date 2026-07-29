package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.spotifyapimodels.SearchResult
import io.ktor.client.*

class SpotifySearchRepo(
    private val client: HttpClient
) {
    suspend fun search(track: String, artist: String?, album: String?, year: String?): SearchResult {
        require(track.isNotBlank())
        TODO()
    }
}