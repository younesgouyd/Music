package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.Tag
import dev.younesgouyd.apps.music.common.TagId
import dev.younesgouyd.apps.music.common.TrackId
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val client: HttpClient
) {
    fun get(id: TagId): Flow<Tag?> {
        TODO()
//        return dao.get(id)
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        TODO()
//        return dao.search(nameQuery.toSearchQuery())
    }

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        TODO()
//        return dao.getTrackTags(id)
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        TODO()
//        return dao.getTrackUnsetTags(id)
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        TODO()
//        val currentTime = System.currentTimeMillis()
//        dao.add(
//            name = name,
//            creationDatetime = currentTime
//        )
    }

    suspend fun delete(id: TagId) {
        TODO()
//        dao.delete(id)
    }
}