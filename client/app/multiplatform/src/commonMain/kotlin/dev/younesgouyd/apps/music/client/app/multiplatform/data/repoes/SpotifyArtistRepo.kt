package dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos.SpotifyArtistDao
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.SpotifyArtist
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.toSearchQuery
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Offset
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val dao: SpotifyArtistDao
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return dao.get(id)
    }

    suspend fun search(nameQuery: String, limit: Int, offset: Offset.Id<SpotifyArtistId>): List<SpotifyArtist> {
        return dao.search(
            nameQuery = nameQuery.toSearchQuery(),
            limit = limit,
            lastId = offset.value ?: SpotifyArtistId(
                0
            )
        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyTrackSpotifyArtists(id)
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyAlbumSpotifyArtists(id)
    }
}