package dev.younesgouyd.apps.music.client

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.Server
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.data.room.transactions.Import
import dev.younesgouyd.apps.music.client.util.getFileName
import dev.younesgouyd.apps.music.client.util.getInputStream
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
        println("starting ImportService")
        coroutineScope.launch {
            importSessionItemRepo.getOldestPending().collect { session ->
                if (session == null) {
                    return@collect
                }
                if (currentSessionItemId == session.id) {
                    println("Session item ${session.id} was already started")
                    return@collect
                }
                importSessionItemRepo.updateState(session.id, ImportSessionItem.State.InProgress, null)
                currentSessionItemId = session.id
                val sessionState = importSessionItemRepo.get(session.id).map { it?.state }
                launch {
                    sessionState.collect { state ->
                        when (state) {
                            null -> {
                                println("Session item ${session.id} null → return@collect")
                                return@collect
                            }
                            ImportSessionItem.State.Nonselected -> {
                                println("Session item ${session.id} Nonselected → NotImplementedError")
                                TODO()
                            }
                            ImportSessionItem.State.Pending -> {
                                println("Session item ${session.id} Pending → NotImplementedError")
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
                                            println("import work for Session item ${session.id} was cancelled as intended")
                                        } else {
                                            cancellation.printStackTrace()
                                            TODO()
                                        }
                                    } catch (e: Exception) {
                                        importSessionItemRepo.updateState(session.id, ImportSessionItem.State.Failed, null)
                                        e.printStackTrace()
                                    }
                                }
                            }
                            ImportSessionItem.State.Completed -> {
                                println("Session item ${session.id} Completed → stopping")
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            ImportSessionItem.State.Cancelled -> {
                                println("Session item ${session.id} Cancelled → stopping")
                                currentSessionItemId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }
                            ImportSessionItem.State.Failed -> {
                                println("Session item ${session.id} Failed → stopping")
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
        println("stopping ImportService")
        coroutineScope.cancel(message = "IntendedCancellation", cause = IntendedCancellation())
        if (currentSessionItemId != null) {
            importSessionItemRepo.updateState(currentSessionItemId!!, ImportSessionItem.State.Failed, null)
        }
    }

    private suspend fun import(session: ImportSession, item: ImportSessionItem) {
        transaction.execute(
            session = session,
            item = item,
            server = server,
            fileManager = fileManager,
            getFileName = ::getFileName,
            getFileInputStream = ::getInputStream
        )
    }

    private class IntendedCancellation : Throwable(null, null, false, false) {
        private fun readResolve(): Any = IntendedCancellation() // TODO (this was recommended by ide)
    }
}