package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.server.common.data.room.daos.SpotifyArtistDao
import dev.younesgouyd.apps.music.server.common.data.room.entities.SpotifyArtist
import dev.younesgouyd.apps.music.server.common.data.room.toSearchQuery
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
            lastId = offset.value ?: SpotifyArtistId(0)
        )
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyTrackSpotifyArtists(id)
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyAlbumSpotifyArtists(id)
    }
}