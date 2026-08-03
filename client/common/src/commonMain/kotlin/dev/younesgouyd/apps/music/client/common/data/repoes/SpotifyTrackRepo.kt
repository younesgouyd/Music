package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackRelation
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyTrackRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpotifyTrackRepo(
    private val backend: Backend
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        return backend.call(
            SpotifyTrackRpc.GetId(spotifyId)
        ).body<SpotifyTrackId?>()
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        return flow {
            emit(
                backend.call(
                    SpotifyTrackRpc.GetAlbumTracks(id)
                ).body<List<SpotifyTrackRelation>>()
            )
        }
    }
}