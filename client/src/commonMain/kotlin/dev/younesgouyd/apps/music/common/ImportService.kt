package dev.younesgouyd.apps.music.common

import dev.younesgouyd.apps.music.common.data.Server
import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionWithItems
import dev.younesgouyd.apps.music.common.usecases.Import
import dev.younesgouyd.apps.music.common.usecases.ImportFolderUseCase
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import kotlin.concurrent.Volatile

class ImportService(
    private val server: Server,
    private val repo: ImportSessionRepo,
    private val importFolderUseCase: ImportFolderUseCase
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var started: Boolean = false
    @Volatile
    private var currentSessionId: Long? = null
    @Volatile
    private var importJob: Job? = null

    fun start() {
        if (started) return
        else started = true
        println("starting ImportService")
        coroutineScope.launch {
            repo.getOldestPending().collect { sessionWithItems ->
                if (sessionWithItems == null) {
                    return@collect
                }
                val session: ImportSession = sessionWithItems.importSession
                if (currentSessionId == session.id) {
                    println("session ${session.id} was already started")
                    return@collect
                }
                repo.updateState(session.id, ImportSessionState.Started)
                currentSessionId = session.id
                val sessionState = repo.get(session.id).map { it.importSession.state }
                launch {
                    sessionState.collect { state ->
                        when (state) {
                            ImportSessionState.Started -> {
                                if (importJob != null) {
                                    return@collect
                                }
                                importJob = launch {
                                    try {
                                        if (import(sessionWithItems)) {
                                            repo.updateState(session.id, ImportSessionState.Completed)
                                        } else {
                                            repo.updateState(session.id, ImportSessionState.Failed)
                                        }
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
        coroutineScope.cancel(message = "IntendedCancellation", cause = IntendedCancellation)
        if (currentSessionId != null) {
            repo.updateState(currentSessionId!!, ImportSessionState.Failed)
        }
    }

    private suspend fun import(session: ImportSessionWithItems): Boolean {
        return withContext(Dispatchers.IO) {
            println("Working on session ${session.importSession.id}")
            when (session.importSession.sourceType) {
                ImportSourceType.Local -> importLocal(session)
                ImportSourceType.Internet -> importInternet(session)
            }
        }
    }

    private suspend fun importInternet(session: ImportSessionWithItems): Boolean {
        val result: String = server.download(session.items.map { it.itemId }).first()
        return when (result) {
            "error" -> false
            "completed" -> {
                val folder = server.getResult()
                importFolderUseCase.execute(
                    Import.Internet(
                        folderUri = folder.toURI().toString(),
                        url = session.importSession.uri,
                        items = json.decodeFromString<List<Inspection.Item>>(
                            withContext(Dispatchers.IO) {
                                File(folder, "index.json").readText()
                            }
                        )
                    )
                )
            }
            else -> TODO()
        }
    }

    private suspend fun importLocal(session: ImportSessionWithItems): Boolean {
        return importFolderUseCase.execute(
            Import.Local(session.importSession.uri)
        )
    }

    private object IntendedCancellation : Throwable() {
        private fun readResolve(): Any = IntendedCancellation // TODO (this was recommended by ide)
    }
}