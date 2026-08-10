package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAlbum
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAlbumRpc
import kotlinx.coroutines.flow.Flow

class SpotifyAlbumRepo(
    private val backend: Backend
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        return backend.stream(SpotifyAlbumRpc.Get(id))
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        return backend.stream(
            SpotifyAlbumRpc.SearchArtist(
                id = id,
                nameQuery = nameQuery
            )
        )
    }
}