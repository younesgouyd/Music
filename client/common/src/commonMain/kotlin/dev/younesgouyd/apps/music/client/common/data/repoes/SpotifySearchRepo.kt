package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.rpc.SpotifySearchRpc
import dev.younesgouyd.apps.music.common.models.spotify.SearchResult
import io.ktor.client.call.*

class SpotifySearchRepo(
    private val backend: Backend
) {
    suspend fun search(track: String, artist: String?, album: String?, year: String?): SearchResult {
        require(track.isNotBlank())
        return backend.call(
            SpotifySearchRpc.Search(
                track = track,
                artist = artist,
                album = album,
                year = year
            )
        ).body<SearchResult>()
    }
}