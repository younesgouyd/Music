package dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos.SpotifyAlbumDao
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.SpotifyAlbum
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.toSearchQuery
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