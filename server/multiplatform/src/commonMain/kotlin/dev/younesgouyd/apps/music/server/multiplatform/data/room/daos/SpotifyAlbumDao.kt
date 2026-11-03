package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyAlbum
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SpotifyAlbumDao {
    @Query("select * from spotifyalbum where id = :id")
    abstract fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum?>

    @Query("""
        select a.*
        from spotifyalbum a
        join spotifyartistspotifyalbumcrossref cr on cr.spotifyAlbumId = a.id
        where cr.spotifyArtistId = :id
        and a.name like :nameQuery
    """)
    abstract fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>>
}