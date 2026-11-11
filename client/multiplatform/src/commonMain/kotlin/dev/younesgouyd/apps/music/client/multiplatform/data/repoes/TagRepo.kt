package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.Tag
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val server: Server
) {
    fun get(id: TagId): Flow<Tag?> {
        return server.observe(Rpc.ToServer.Tag.Get(id))
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return server.observe(Rpc.ToServer.Tag.Search(nameQuery))
    }

    fun searchTrackTags(id: TrackId, nameQuery: String): Flow<List<Tag>> {
        return server.observe(Rpc.ToServer.Tag.SearchTrackTags(id, nameQuery))
    }

    fun searchTrackUnsetTags(id: TrackId, nameQuery: String): Flow<List<Tag>> {
        return server.observe(Rpc.ToServer.Tag.SearchTrackUnsetTags(id, nameQuery))
    }

    suspend fun add(name: String): TagId {
        require(name.isNotBlank())
        return server.get(Rpc.ToServer.Tag.Add(name))
    }

    suspend fun delete(id: TagId) {
        server.call(Rpc.ToServer.Tag.Delete(id))
    }
}