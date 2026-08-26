package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.SpotifyAlbum
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAlbumRpc
import kotlinx.coroutines.flow.Flow

class SpotifyAlbumRepo(
    private val server: Server
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        return server.stream(SpotifyAlbumRpc.Get(id))
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        return server.stream(
            SpotifyAlbumRpc.SearchArtist(
                id = id,
                nameQuery = nameQuery
            )
        )
    }
}