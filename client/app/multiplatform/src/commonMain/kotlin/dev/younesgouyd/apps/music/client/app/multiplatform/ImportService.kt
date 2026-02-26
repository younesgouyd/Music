package dev.younesgouyd.apps.music.client.app.multiplatform

import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.Server
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.Import
import dev.younesgouyd.apps.music.client.app.multiplatform.util.getFileName
import dev.younesgouyd.apps.music.client.app.multiplatform.util.getInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class ImportService(
    private val importSessionRepo: ImportSessionRepo,
    private val importSessionItemRepo: ImportSessionItemRepo,
    private val transaction: Import,
    private val server: Server,
    private val fileManager: FileManager
) {
    private val coroutineScope = Music.coroutineScope
    private var started: Boolean = false
    private val logger = KotlinLogging.logger {}

    @Volatile
    private var currentSessionItemId: ImportSessionItemId? = null

    @Volatile
    private var importJob: Job? = null

    fun start() {
        if (started) {
            TODO()
        } else {
            started = true
        }
        logger.info { "starting ImportService" }
        coroutineScope.launch {
            importSessionItemRepo.getOldestPending().collect { session ->
                if (session == null) {
                    return@collect
                }
                if (currentSessionItemId == session.id) {
                    logger.info { "Session item ${session.id} was already started" }
                    return@collect
                }
                importSessionItemRepo.updateState(session.id, ImportSessionItem.State.InProgress, null)
                currentSessionItemId = session.id
                val sessionState = importSessionItemRepo.get(session.id).map { it?.state }
                launch {
                    sessionState.collect { state ->
                        when (state) {
                            null -> {
                                logger.info { "Session item ${session.id} null → return@collect" }
                                return@collect
                            }
                            ImportSessionItem.State.Nonselected -> {
                                logger.error { "Session item ${session.id} Nonselected → NotImplementedError" }
                                TODO()
                            }
                            ImportSessionItem.State.Pending -> {
                                logger.error { "Session item ${session.id} Pending → NotImplementedError" }
                                TODO()
                            }
                            ImportSessionItem.State.InProgress -> {
                                if (importJob != null) {
                                    return@collect
                                }
                                importJob = launch {
                                    try {
                                        import(
                                            session = importSessionRepo.get(session.importSessionId).first()!!,
                                            item = session
                                        )
                                    } catch (cancellation: CancellationException) {
                                        if (cancellation.cause is IntendedCancellation) {
                                            logger.info(cancellation) { "import work was cancelled as intended" }
                                        } else {
                                            logger.error(cancellation) { "import work was not cancelled as intended" }
                                            TODO()
                                        }
                                    } catch (e: Exception) {
                                        logger.error(e) { "something went wrong while importing" }
                                        importSessionItemRepo.updateState(session.id, ImportSessionItem.State.Failed, null)
                                    }
                                }
                            }
                            ImportSessionItem.State.Completed -> {
                                logger.info { "Session item ${session.id} Completed → stopping" }
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            ImportSessionItem.State.Cancelled -> {
                                logger.info { "Session item ${session.id} Cancelled → stopping" }
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            ImportSessionItem.State.Failed -> {
                                logger.info { "Session item ${session.id} Failed → stopping" }
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                        }
                    }
                }.join()
            }
        }
    }

    suspend fun stop() {
        logger.info { "stopping ImportService" }
        coroutineScope.cancel(message = "IntendedCancellation", cause = IntendedCancellation())
        if (currentSessionItemId != null) {
            importSessionItemRepo.updateState(currentSessionItemId!!, ImportSessionItem.State.Failed, null)
        }
    }

    private suspend fun import(session: ImportSession, item: ImportSessionItem) {
        logger.info { "Working on session ${session.id} item ${item.id}" }
        transaction.execute(
            session = session,
            item = item,
            server = server,
            fileManager = fileManager,
            getFileName = ::getFileName,
            getFileInputStream = ::getInputStream
        )
    }

    private class IntendedCancellation : Throwable() {
        private fun readResolve(): Any = IntendedCancellation() // TODO (this was recommended by ide)
    }
}