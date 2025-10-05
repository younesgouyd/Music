package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.common.data.room.entities.MediaFileDao
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaFileRepo(private val dao: MediaFileDao) {
    fun getAny(id: Long): Flow<MediaFile> {
        return dao.getTrackMediaFiles(id).map { it.first() }
    }

    suspend fun add(
        name: String,
        trackId: Long,
        sourceUri: String,
        sourceWebpageUrl: String?,
        sourceType: ImportSourceType
    ): Long {
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            trackId = trackId,
            sourceUri = sourceUri,
            sourceWebpageUrl = sourceWebpageUrl,
            sourceType = sourceType,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }
}