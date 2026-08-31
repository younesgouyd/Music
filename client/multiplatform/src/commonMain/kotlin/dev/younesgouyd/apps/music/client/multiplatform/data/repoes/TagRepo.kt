package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Tag
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.TagRpc
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val server: Server
) {
    fun get(id: TagId): Flow<Tag?> {
        return server.observe(TagRpc.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return server.observe(TagRpc.Search(nameQuery))
    }

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        return server.observe(TagRpc.GetTrackTags(id))
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        return server.observe(TagRpc.GetTrackUnsetTags(id))
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        server.call<Unit>(TagRpc.Add(name))
    }

    suspend fun delete(id: TagId) {
        server.call<Unit>(TagRpc.Delete(id))
    }
}