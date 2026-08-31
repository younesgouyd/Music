package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyArtistRpc
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val server: Server
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return server.observe(SpotifyArtistRpc.Get(id))
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        return server.call(
            SpotifyArtistRpc.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return server.observe(SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists(id))
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return server.observe(SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists(id))
    }
}