package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.TagId
import dev.younesgouyd.apps.music.common.TrackId
import io.ktor.client.*

class TagTrackCrossRefRepo(
    private val client: HttpClient
) {
    suspend fun add(tagId: TagId, trackId: TrackId) {
        TODO()
//        dao.add(
//            tagId = tagId,
//            trackId = trackId,
//            creationDatetime = System.currentTimeMillis()
//        )
    }

    suspend fun delete(tagId: TagId, trackId: TrackId) {
        TODO()
//        dao.delete(
//            tagId = tagId,
//            trackId = trackId
//        )
    }
}