package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.spotify.SearchResult

class SpotifySearchRepo(
    private val server: Server
) {
    suspend fun search(track: String, artist: String?, album: String?, year: String?): SearchResult {
        require(track.isNotBlank())
        return server.get(
            Rpc.ToServer.SpotifySearch.Search(
                track = track,
                artist = artist,
                album = album,
                year = year
            )
        )
    }
}