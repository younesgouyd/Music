package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.*
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val client: HttpClient
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        TODO()
//        return dao.get(id)
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        TODO()
//        return dao.search(
//            nameQuery = nameQuery.toSearchQuery(),
//            limit = limit,
//            lastId = offset.value ?: SpotifyArtistId(0)
//        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        TODO()
//        return dao.getSpotifyTrackSpotifyArtists(id)
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        TODO()
//        return dao.getSpotifyAlbumSpotifyArtists(id)
    }
}