package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Media_file
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.MediaFileQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaFileRepo(private val queries: MediaFileQueries) {
    suspend fun getStatic(id: Long): Media_file {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOne()
        }
    }

    suspend fun add(name: String, sourceType: SourceType, domainName: String?): Long {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            queries.add(
                name = name,
                source_type = sourceType.name,
                domain_name = domainName,
                creation_datetime = currentTime,
                update_datetime = currentTime
            ).executeAsOne()
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }

    enum class SourceType { Local, Internet }
}