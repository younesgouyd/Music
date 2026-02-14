package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.room.entities.SpotifyArtist
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SpotifyArtistDao {
    @Query("select * from spotifyartist where id = :id")
    abstract fun get(id: SpotifyArtistId): Flow<SpotifyArtist?>

    @Query("select * from spotifyartist where name like :nameQuery")
    abstract fun search(nameQuery: String): Flow<List<SpotifyArtist>>

    @Query("""
        select a.*
        from spotifyartist a
        join spotifyartistspotifytrackcrossref cr on cr.spotifyArtistId = a.id
        where cr.spotifyTrackId = :id
    """)
    abstract fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>>

    @Query("""
        select a.*
        from spotifyartist a
        join spotifyartistspotifyalbumcrossref cr on cr.spotifyArtistId = a.id
        where cr.spotifyAlbumId = :id
    """)
    abstract fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>>
}