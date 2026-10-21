package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId

class TagTrackCrossRefRepo(
    private val server: Server
) {
    suspend fun add(tagId: TagId, trackId: TrackId) {
        server.call(Rpc.ToServer.TagTrackCrossRef.Add(tagId, trackId))
    }

    suspend fun delete(tagId: TagId, trackId: TrackId) {
        server.call(Rpc.ToServer.TagTrackCrossRef.Delete(tagId, trackId))
    }
}