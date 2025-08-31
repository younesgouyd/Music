package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Media_file
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.MediaFileQueries
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaFileRepo(private val queries: MediaFileQueries) {
    suspend fun getStatic(id: Long): Media_file {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOne()
        }
    }

    suspend fun add(name: String, importSourceType: ImportSourceType, domainName: String?): Long {
        return withContext(Dispatchers.IO) {
            require(
                (importSourceType == ImportSourceType.Local && domainName == null)
                || (importSourceType == ImportSourceType.Internet && !domainName.isNullOrBlank())
            )
            val currentTime = System.currentTimeMillis()
            queries.add(
                name = name,
                source_type = importSourceType.name,
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
}