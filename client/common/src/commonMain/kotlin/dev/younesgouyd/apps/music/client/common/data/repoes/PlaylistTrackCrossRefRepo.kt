package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistTrackCrossRefRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlaylistTrackCrossRefRepo(
    private val backend: Backend
) {
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?> {
        return flow {
            emit(
                backend.call(
                    PlaylistTrackCrossRefRpc.Get(
                        playlistId = playlistId,
                        trackId = trackId
                    )
                ).body<PlaylistTrackCrossRef?>()
            )
        }
    }

    suspend fun add(playlistId: PlaylistId, trackId: TrackId) {
        backend.call(
            PlaylistTrackCrossRefRpc.Add(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }

    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        backend.call(
            PlaylistTrackCrossRefRpc.ChangeItemPosition(
                playlistId = playlistId,
                from = from,
                to = to
            )
        )
    }

    suspend fun delete(playlistId: PlaylistId, trackId: TrackId) {
        backend.call(
            PlaylistTrackCrossRefRpc.Delete(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }
}