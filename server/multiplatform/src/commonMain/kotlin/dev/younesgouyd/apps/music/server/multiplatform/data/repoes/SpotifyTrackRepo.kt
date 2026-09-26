package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.SpotifyTrackDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.SpotifyTrackRelation
import kotlinx.coroutines.flow.Flow

class SpotifyTrackRepo(
    private val dao: SpotifyTrackDao
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        return dao.getId(spotifyId)?.let {
            SpotifyTrackId(it)
        }
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        return dao.getAlbumTracks(id)
    }
}