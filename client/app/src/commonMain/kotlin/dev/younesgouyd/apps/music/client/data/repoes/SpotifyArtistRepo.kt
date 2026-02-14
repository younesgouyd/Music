package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.room.daos.SpotifyArtistDao
import dev.younesgouyd.apps.music.client.data.room.entities.SpotifyArtist
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val dao: SpotifyArtistDao
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return dao.get(id)
    }

    fun search(nameQuery: String): Flow<List<SpotifyArtist>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyTrackSpotifyArtists(id)
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyAlbumSpotifyArtists(id)
    }
}