package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyArtist
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val server: Server
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return server.observe(Rpc.ToServer.SpotifyArtist.Get(id))
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        return server.get(
            Rpc.ToServer.SpotifyArtist.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return server.observe(Rpc.ToServer.SpotifyArtist.GetSpotifyTrackSpotifyArtists(id))
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return server.observe(Rpc.ToServer.SpotifyArtist.GetSpotifyAlbumSpotifyArtists(id))
    }
}