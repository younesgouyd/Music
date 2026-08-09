package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyArtistRpc
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val backend: Backend
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return backend.stream(SpotifyArtistRpc.Get(id))
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        return backend.callForResult(
            SpotifyArtistRpc.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return backend.stream(SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists(id))
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return backend.stream(SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists(id))
    }
}