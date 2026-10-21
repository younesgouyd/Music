package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyAlbum
import kotlinx.coroutines.flow.Flow

class SpotifyAlbumRepo(
    private val server: Server
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        return server.observe(Rpc.ToServer.SpotifyAlbum.Get(id))
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        return server.observe(
            Rpc.ToServer.SpotifyAlbum.SearchArtist(
                id = id,
                nameQuery = nameQuery
            )
        )
    }
}