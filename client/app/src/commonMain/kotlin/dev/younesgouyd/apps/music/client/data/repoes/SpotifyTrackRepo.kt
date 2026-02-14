package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.room.daos.SpotifyTrackDao
import dev.younesgouyd.apps.music.client.data.room.entities.SpotifyTrackRelation
import kotlinx.coroutines.flow.Flow

class SpotifyTrackRepo(
    private val dao: SpotifyTrackDao
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        return dao.getId(spotifyId)?.let { SpotifyTrackId(it) }
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        return dao.getAlbumTracks(id)
    }
}