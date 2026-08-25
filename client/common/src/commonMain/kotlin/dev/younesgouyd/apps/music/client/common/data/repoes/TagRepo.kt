package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.Tag
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.TagRpc
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val server: Server
) {
    fun get(id: TagId): Flow<Tag?> {
        return server.stream(TagRpc.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return server.stream(TagRpc.Search(nameQuery))
    }

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        return server.stream(TagRpc.GetTrackTags(id))
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        return server.stream(TagRpc.GetTrackUnsetTags(id))
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        server.call(TagRpc.Add(name))
    }

    suspend fun delete(id: TagId) {
        server.call(TagRpc.Delete(id))
    }
}