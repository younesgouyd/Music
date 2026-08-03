package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
sealed class TagTrackCrossRefRpc : Rpc() {
    @Serializable
    data class Add(
        val tagId: TagId,
        val trackId: TrackId
    ) : TagTrackCrossRefRpc()

    @Serializable
    data class Delete(
        val tagId: TagId,
        val trackId: TrackId
    ) : TagTrackCrossRefRpc()
}