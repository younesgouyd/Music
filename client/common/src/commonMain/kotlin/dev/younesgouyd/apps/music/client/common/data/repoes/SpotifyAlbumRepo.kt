package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAlbum
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAlbumRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpotifyAlbumRepo(
    private val backend: Backend
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        return flow {
            emit(
                backend.call(
                    SpotifyAlbumRpc.Get(id)
                ).body<SpotifyAlbum?>()
            )
        }
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        return flow {
            emit(
                backend.call(
                    SpotifyAlbumRpc.SearchArtist(
                        id = id,
                        nameQuery = nameQuery
                    )
                ).body<List<SpotifyAlbum>>()
            )
        }
    }
}