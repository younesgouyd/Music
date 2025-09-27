package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Media_file
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.MediaFileQueries
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaFileRepo(private val queries: MediaFileQueries) {
    suspend fun getAnyStatic(id: Long): Media_file {
        return withContext(Dispatchers.IO) {
            queries.getTrackMediaFiles(id)
                .executeAsList()
                .first()
        }
    }

    suspend fun add(
        name: String,
        trackId: Long,
        sourceUri: String,
        sourceWebpageUrl: String?,
        sourceType: ImportSourceType
    ): Long {
        return withContext(Dispatchers.IO) {
            queries.add(
                name = name,
                track_id = trackId,
                source_uri = sourceUri,
                source_webpage_url = sourceWebpageUrl,
                source_type = sourceType.name,
                creation_datetime = System.currentTimeMillis()
            ).executeAsOne()
        }
    }
}