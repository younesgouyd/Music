package dev.younesgouyd.apps.music.server.common.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.server.common.data.room.entities.SpotifyArtist
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SpotifyArtistDao {
    @Query("select * from spotifyartist where id = :id")
    abstract fun get(id: SpotifyArtistId): Flow<SpotifyArtist?>

    @Query("""
        select *
        from spotifyartist
        where id > :lastId
        and name like :nameQuery
        order by id asc
        limit :limit
    """)
    abstract suspend fun search(
        nameQuery: String,
        limit: Int,
        lastId: SpotifyArtistId
    ): List<SpotifyArtist>

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