package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.TagDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Tag
import dev.younesgouyd.apps.music.server.multiplatform.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val dao: TagDao
) {
    fun get(id: TagId): Flow<Tag?> {
        return dao.get(id)
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun searchTrackTags(id: TrackId, nameQuery: String): Flow<List<Tag>> {
        return dao.searchTrackTags(id, nameQuery.toSearchQuery())
    }

    fun searchTrackUnsetTags(id: TrackId, nameQuery: String): Flow<List<Tag>> {
        return dao.searchTrackUnsetTags(id, nameQuery.toSearchQuery())
    }

    suspend fun add(name: String): TagId {
        require(name.isNotBlank())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name,
            creationDatetime = currentTime
        )
        return TagId(id)
    }

    suspend fun delete(id: TagId) {
        dao.delete(id)
    }
}