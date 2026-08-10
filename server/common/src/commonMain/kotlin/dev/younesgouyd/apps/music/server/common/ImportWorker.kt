package dev.younesgouyd.apps.music.server.common

import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.server.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.common.data.room.transactions.Import
import dev.younesgouyd.apps.music.server.common.usecases.getFileName
import dev.younesgouyd.apps.music.server.common.usecases.getInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile

class ImportWorker(
    private val importSessionRepo: ImportSessionRepo,
    private val importSessionItemRepo: ImportSessionItemRepo,
    private val transaction: Import,
    private val ytDlp: YtDlp,
    private val fileManager: FileManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var started: Boolean = false
    private val logger = KotlinLogging.logger {}

    @Volatile
    private var currentSessionItemId: ImportSessionItemId? = null

    @Volatile
    private var importJob: Job? = null

    fun start() {
        logger.info { "--> ImportWorker::start" }
        if (started) {
            TODO()
        } else {
            started = true
        }
        coroutineScope.launch {
            importSessionItemRepo.getOldestPending().collect { session ->
                if (session == null) {
                    return@collect
                }
                if (currentSessionItemId == session.id) {
                    logger.info { "Session item ${session.id} was already started" }
                    return@collect
                }
                importSessionItemRepo.updateState(session.id, dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.InProgress, null)
                currentSessionItemId = session.id
                val sessionState = importSessionItemRepo.get(session.id).map { it?.state }
                launch {
                    sessionState.collect { state ->
                        when (state) {
                            null -> {
                                logger.info { "Session item ${session.id} null → return@collect" }
                                return@collect
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Nonselected -> {
                                logger.error { "Session item ${session.id} Nonselected → NotImplementedError" }
                                TODO()
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Pending -> {
                                logger.error { "Session item ${session.id} Pending → NotImplementedError" }
                                TODO()
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.InProgress -> {
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
                                        importSessionItemRepo.updateState(session.id, dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Failed, null)
                                    }
                                }
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Completed -> {
                                logger.info { "Session item ${session.id} Completed → stopping" }
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Cancelled -> {
                                logger.info { "Session item ${session.id} Cancelled → stopping" }
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Failed -> {
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
        logger.info { "<-- ImportWorker::start" }
    }

    suspend fun stop() {
        logger.info { "--> ImportWorker::stop" }
        coroutineScope.cancel(message = "IntendedCancellation", cause = IntendedCancellation())
        if (currentSessionItemId != null) {
            importSessionItemRepo.updateState(currentSessionItemId!!, dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Failed, null)
        }
        logger.info { "<-- ImportWorker::stop" }
    }

    private suspend fun import(session: ImportSession, item: ImportSessionItem) {
        logger.info { "Working on session ${session.id} item ${item.id}" }
        transaction.execute(
            session = session,
            item = item,
            ytDlp = ytDlp,
            fileManager = fileManager,
            getFileName = ::getFileName,
            getFileInputStream = ::getInputStream
        )
    }

    private class IntendedCancellation : Throwable() {
        private fun readResolve(): Any = IntendedCancellation() // TODO (this was recommended by ide)
    }
}