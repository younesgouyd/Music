package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TrackId

class PlaylistTrackCrossRefRepo(
    private val server: Server
) {
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