package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.SpotifyArtistDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyArtist
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val dao: SpotifyArtistDao
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return dao.get(id)
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyTrackSpotifyArtists(id)
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return dao.getSpotifyAlbumSpotifyArtists(id)
    }
}