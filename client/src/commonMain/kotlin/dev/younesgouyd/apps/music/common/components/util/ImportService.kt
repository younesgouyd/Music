package dev.younesgouyd.apps.music.common.components.util

import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Import_session
import dev.younesgouyd.apps.music.common.usecases.ImportFolderUseCase
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile

class ImportService(
    private val repo: ImportSessionRepo,
    private val importFolderUseCase: ImportFolderUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var started: Boolean = false
    @Volatile private var currentSessionId: Long? = null
    @Volatile private var importJob: Job? = null

    fun start() {
        if (started) return
        else started = true
        println("starting ImportService")
        scope.launch {
            repo.getOldestPending().collect { session ->
                if (session == null) {
                    return@collect
                }
                if (currentSessionId == session.id) {
                    println("session ${session.id} was already started")
                    return@collect
                }
                repo.updateState(session.id, ImportSessionState.Started)
                currentSessionId = session.id
                val sessionState = repo.get(session.id).map { ImportSessionState.valueOf(it.state) }
                launch {
                    sessionState.collect { state ->
                        when (state) {
                            ImportSessionState.Started -> {
                                if (importJob != null) {
                                    return@collect
                                }
                                importJob = launch {
                                    try {
                                        import(session)
                                        repo.updateState(session.id, ImportSessionState.Completed)
                                    } catch (cancellation: CancellationException) {
                                        if (cancellation.cause == IntendedCancellation) {
                                            println("import work for session ${session.id} was cancelled as intended")
                                        } else {
                                            cancellation.printStackTrace()
                                            TODO()
                                        }
                                    } catch (e: Exception) {
                                        repo.updateState(session.id, ImportSessionState.Failed)
                                        e.printStackTrace()
                                    }
                                }
                            }
                            ImportSessionState.Pending -> {
                                println("Session ${session.id} Pending → NotImplementedError")
                                TODO()
                            }
                            ImportSessionState.Completed -> {
                                println("Session ${session.id} Completed → stopping")
                                currentSessionId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation)
                            }
                            ImportSessionState.Cancelled -> {
                                println("Session ${session.id} Cancelled → stopping")
                                currentSessionId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation)
                            }
                            ImportSessionState.Failed -> {
                                println("Session ${session.id} Failed → stopping")
                                currentSessionId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation)
                            }
                        }
                    }
                }.join()
            }
        }
    }

    suspend fun stop() {
        println("stopping ImportService")
        scope.cancel(message = "IntendedCancellation", cause = IntendedCancellation)
        if (currentSessionId != null) {
            repo.updateState(currentSessionId!!, ImportSessionState.Failed)
        }
    }

    private suspend fun import(session: Import_session) {
        withContext(Dispatchers.IO) {
            println("Working on session ${session.id}")
            when (ImportSourceType.valueOf(session.source_type)) {
                ImportSourceType.Local -> importFolderUseCase.execute(session.uri)
                ImportSourceType.Internet -> TODO()
            }
        }
    }

    private object IntendedCancellation : Throwable() {
        private fun readResolve(): Any = IntendedCancellation // TODO (this was recommended by ide)
    }
}
