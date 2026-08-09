package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackRelation
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyTrackRpc
import kotlinx.coroutines.flow.Flow

class SpotifyTrackRepo(
    private val backend: Backend
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        return backend.callForResult(
            SpotifyTrackRpc.GetId(spotifyId)
        )
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        return backend.stream(SpotifyTrackRpc.GetAlbumTracks(id))
    }
}