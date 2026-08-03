package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyArtistRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpotifyArtistRepo(
    private val backend: Backend
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return flow {
            emit(
                backend.call(
                    SpotifyArtistRpc.Get(id)
                ).body<SpotifyArtist?>()
            )
        }
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        return backend.call(
            SpotifyArtistRpc.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        ).body<List<SpotifyArtist>>()
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return flow {
            emit(
                backend.call(
                    SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists(id)
                ).body<List<SpotifyArtist>>()
            )
        }
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return flow {
            emit(
                backend.call(
                    SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists(id)
                ).body<List<SpotifyArtist>>()
            )
        }
    }
}