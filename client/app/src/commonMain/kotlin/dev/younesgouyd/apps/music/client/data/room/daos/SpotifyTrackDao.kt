package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.room.entities.SpotifyTrackRelation
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SpotifyTrackDao {
    @Query("select id from spotifytrack where spotifyId = :spotifyId")
    abstract suspend fun getId(spotifyId: String): Long?

    @Transaction
    @Query("select * from spotifytrack where spotifyAlbumId = :id order by discNumber, trackNumber")
    abstract fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>>
}