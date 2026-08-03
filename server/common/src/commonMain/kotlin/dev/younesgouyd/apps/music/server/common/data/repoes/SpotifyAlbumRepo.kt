package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.server.common.data.room.daos.SpotifyAlbumDao
import dev.younesgouyd.apps.music.server.common.data.room.entities.SpotifyAlbum
import dev.younesgouyd.apps.music.server.common.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class SpotifyAlbumRepo(
    private val dao: SpotifyAlbumDao
) {
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?> {
        return dao.get(id)
    }

    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>> {
        return dao.searchArtist(id, nameQuery.toSearchQuery())
    }
}