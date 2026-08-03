package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.Tag
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.rpc.TagRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TagRepo(
    private val backend: Backend
) {
    fun get(id: TagId): Flow<Tag?> {
        return flow {
            emit(
                backend.call(TagRpc.Get(id)).body<Tag?>()
            )
        }
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return flow {
            emit(
                backend.call(TagRpc.Search(nameQuery)).body<List<Tag>>()
            )
        }
    }

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        return flow {
            emit(
                backend.call(TagRpc.GetTrackTags(id)).body<List<Tag>>()
            )
        }
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        return flow {
            emit(
                backend.call(TagRpc.GetTrackUnsetTags(id)).body<List<Tag>>()
            )
        }
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        backend.call(TagRpc.Add(name))
    }

    suspend fun delete(id: TagId) {
        backend.call(TagRpc.Delete(id))
    }
}