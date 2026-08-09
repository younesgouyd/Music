package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.TagTrackCrossRefRpc

class TagTrackCrossRefRepo(
    private val backend: Backend
) {
    suspend fun add(tagId: TagId, trackId: TrackId) {
        backend.call(TagTrackCrossRefRpc.Add(tagId, trackId))
    }

    suspend fun delete(tagId: TagId, trackId: TrackId) {
        backend.call(TagTrackCrossRefRpc.Delete(tagId, trackId))
    }
}