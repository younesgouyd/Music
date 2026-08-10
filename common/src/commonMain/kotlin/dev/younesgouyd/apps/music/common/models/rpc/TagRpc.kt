package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
sealed class TagRpc : Rpc() {
    @Serializable
    data class Get(
        val id: TagId
    ) : TagRpc()

    @Serializable
    data class Search(
        val nameQuery: String
    ) : TagRpc()

    @Serializable
    data class GetTrackTags(
        val id: TrackId
    ) : TagRpc()

    @Serializable
    data class GetTrackUnsetTags(
        val id: TrackId
    ) : TagRpc()

    @Serializable
    data class Add(
        val name: String
    ) : TagRpc()

    @Serializable
    data class Delete(
        val id: TagId
    ) : TagRpc()
}