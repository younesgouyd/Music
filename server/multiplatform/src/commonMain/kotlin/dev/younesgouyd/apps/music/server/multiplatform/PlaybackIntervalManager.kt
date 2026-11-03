package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackContext
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackIntervalStart
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.PlaybackIntervalDao
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

class PlaybackIntervalManager(
    private val dao: PlaybackIntervalDao,
    private val failedDir: File
) {
    private val logger = KotlinLogging.logger {}
    private val mutex = Mutex()
    private val map = mutableMapOf<ClientId, PlaybackIntervalStart>()

    suspend fun add(item: PlaybackInterval) {
        mutex.withLock {
            try {
                val existing = dao.getByUuid(item.uuid)
                val differentInAnythingExceptToAndEndDatetime = existing != null && existing.copyToModel(item.to, item.endDatetime) != item
                if (existing == null || differentInAnythingExceptToAndEndDatetime) {
                    val (uuid, possibleDuplicateOf) = run {
                        if (differentInAnythingExceptToAndEndDatetime) {
                            logger.error { "possible uuid collision or invalid data. existing: $existing, item: $item" }
                            // 2 possible scenarios can result in the same uuid but other properties are different (except "to"
                            // and "endDatetime", which are expected to be different):
                            // - uuid collision.
                            // - bug resulting in different properties between PlaybackIntervalStart and PlaybackInterval.
                            // in this case we add a new record with a new UUID, and mark it as a possible duplicate.
                            Pair(Uuid.random(), existing.id)
                        } else Pair(item.uuid, null)
                    }
                    val currentTime = System.currentTimeMillis()
                    dao.add(
                        uuid = uuid,
                        audioFileId = item.audioFileId,
                        possibleDuplicateOf = possibleDuplicateOf,
                        clientId = item.context.clientId,
                        importId = item.context.importId,
                        trackId = item.context.trackId,
                        playlistId = item.context.playlistId,
                        folderId = item.context.folderId,
                        albumId = item.context.albumId,
                        artistId = item.context.artistId,
                        from = item.from,
                        to = item.to,
                        estimatedTo = null,
                        websocketPingPeriodMs = Application.PING_PERIOD.inWholeMilliseconds,
                        websocketPingTimeoutMs = Application.PING_TIMEOUT.inWholeMilliseconds,
                        startDatetime = item.startDatetime,
                        endDatetime = item.endDatetime,
                        creationDatetime = currentTime,
                        updateDatetime = currentTime
                    )
                } else {
                    dao.update(
                        to = item.to,
                        endDatetime = item.endDatetime,
                        updateDatetime = System.currentTimeMillis(),
                        uuid = item.uuid
                    )
                }
                val clientId = item.context.clientId
                val entry = map[clientId] ?: return
                if (entry.uuid == item.uuid) {
                    map.remove(clientId, entry)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "could not process interval: $item. will try to store it in failed dir"}
                try {
                    withContext(Dispatchers.IO) {
                        File(failedDir, "${item.uuid}_${System.currentTimeMillis()}.json")
                            .writeText(
                                json.encodeToString<PlaybackInterval>(item)
                            )
                    }
                } catch (e: Exception) {
                    logger.error(e) {}
                }
            }
        }
    }

    suspend fun setStart(start: PlaybackIntervalStart) {
        mutex.withLock {
            map[start.clientId] = start
        }
    }

    suspend fun setEnd(clientId: ClientId, disconnectionTimeEpochMilli: Long) {
        mutex.withLock {
            val start = map.remove(clientId) ?: return@withLock
            if (dao.getByUuid(start.uuid) != null) {
                logger.warn { "::setEnd | a record with same uuid already exists" }
                return@withLock
            }
            try {
                val currentTime = System.currentTimeMillis()
                dao.add(
                    uuid = start.uuid,
                    audioFileId = start.audioFileId,
                    possibleDuplicateOf = null,
                    clientId = clientId,
                    importId = start.context.importId,
                    trackId = start.context.trackId,
                    playlistId = start.context.playlistId,
                    folderId = start.context.folderId,
                    albumId = start.context.albumId,
                    artistId = start.context.artistId,
                    from = start.from,
                    to = null,
                    estimatedTo = start.from + (disconnectionTimeEpochMilli - start.datetime),
                    websocketPingPeriodMs = Application.PING_PERIOD.inWholeMilliseconds,
                    websocketPingTimeoutMs = Application.PING_TIMEOUT.inWholeMilliseconds,
                    startDatetime = start.datetime,
                    endDatetime = disconnectionTimeEpochMilli,
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            } catch (e: Exception) {
                logger.error(e) {}
            }
        }
    }

    private fun dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaybackInterval.copyToModel(
        to: Long,
        endDatetime: Long
    ): PlaybackInterval {
        return PlaybackInterval(
            uuid = uuid,
            audioFileId = audioFileId,
            context = PlaybackContext.ClientSent(
                clientId = clientId,
                importId = importId,
                trackId = trackId,
                playlistId = playlistId,
                folderId = folderId,
                albumId = albumId,
                artistId = artistId
            ),
            from = from,
            to = to,
            startDatetime = startDatetime,
            endDatetime = endDatetime
        )
    }
}