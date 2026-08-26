package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackRelation
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyTrackRpc
import kotlinx.coroutines.flow.Flow

class SpotifyTrackRepo(
    private val server: Server
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        return server.callForResult(
            SpotifyTrackRpc.GetId(spotifyId)
        )
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        return server.stream(SpotifyTrackRpc.GetAlbumTracks(id))
    }
}