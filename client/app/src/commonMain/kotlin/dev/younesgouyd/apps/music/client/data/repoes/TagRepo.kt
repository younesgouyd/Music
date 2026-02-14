package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.daos.TagDao
import dev.younesgouyd.apps.music.client.data.room.entities.Tag
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
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

    fun getTrackTags(id: TrackId): Flow<List<Tag>> {
        return dao.getTrackTags(id)
    }

    fun getTrackUnsetTags(id: TrackId): Flow<List<Tag>> {
        return dao.getTrackUnsetTags(id)
    }

    suspend fun add(name: String) {
        require(name.isNotBlank())
        val currentTime = System.currentTimeMillis()
        dao.add(
            name = name,
            creationDatetime = currentTime
        )
    }

    suspend fun delete(id: TagId) {
        dao.delete(id)
    }
}