package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrackCrossRef
import kotlinx.coroutines.flow.Flow

class PlaylistTrackCrossRefRepo(
    private val server: Server
) {
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?> {
        return server.observe(
            Rpc.ToServer.PlaylistTrackCrossRef.Get(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }

    suspend fun add(playlistId: PlaylistId, trackId: TrackId) {
        server.call(
            Rpc.ToServer.PlaylistTrackCrossRef.Add(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }

    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        server.call(
            Rpc.ToServer.PlaylistTrackCrossRef.ChangeItemPosition(
                playlistId = playlistId,
                from = from,
                to = to
            )
        )
    }

    suspend fun delete(playlistId: PlaylistId, trackId: TrackId) {
        server.call(
            Rpc.ToServer.PlaylistTrackCrossRef.Delete(
                playlistId = playlistId,
                trackId = trackId
            )
        )
    }
}