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

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        return server.observe(Rpc.ToServer.Tag.GetTrackTags(id))
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        return server.observe(Rpc.ToServer.Tag.GetTrackUnsetTags(id))
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        server.call(Rpc.ToServer.Tag.Add(name))
    }

    suspend fun delete(id: TagId) {
        server.call(Rpc.ToServer.Tag.Delete(id))
    }
}