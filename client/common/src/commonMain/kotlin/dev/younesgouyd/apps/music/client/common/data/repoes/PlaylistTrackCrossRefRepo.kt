package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRef
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistTrackCrossRefRpc
import kotlinx.coroutines.flow.Flow

class PlaylistTrackCrossRefRepo(
    private val server: Server
) {
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?> {
        return server.stream(
            PlaylistTrackCrossRefRpc.Get(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }

    suspend fun add(playlistId: PlaylistId, trackId: TrackId) {
        server.call(
            PlaylistTrackCrossRefRpc.Add(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }

    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        server.call(
            PlaylistTrackCrossRefRpc.ChangeItemPosition(
                playlistId = playlistId,
                from = from,
                to = to
            )
        )
    }

    suspend fun delete(playlistId: PlaylistId, trackId: TrackId) {
        server.call(
            PlaylistTrackCrossRefRpc.Delete(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }
}