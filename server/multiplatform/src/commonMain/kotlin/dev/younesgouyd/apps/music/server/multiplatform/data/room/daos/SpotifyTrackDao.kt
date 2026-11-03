package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.SpotifyTrackRelation
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SpotifyTrackDao {
    @Query("select id from spotifytrack where spotifyId = :spotifyId")
    abstract suspend fun getId(spotifyId: String): Long?

    @Transaction
    @Query("select * from spotifytrack where spotifyAlbumId = :id order by discNumber, trackNumber")
    abstract fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>>
}