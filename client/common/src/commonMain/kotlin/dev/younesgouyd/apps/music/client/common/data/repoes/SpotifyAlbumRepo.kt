package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.SpotifyAlbum
import dev.younesgouyd.apps.music.common.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.SpotifyArtistId
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class SpotifyAlbumRepo(
    private val client: HttpClient
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        TODO()
//        return dao.get(id)
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        TODO()
//        return dao.searchArtist(id, nameQuery.toSearchQuery())
    }
}