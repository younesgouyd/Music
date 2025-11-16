package dev.younesgouyd.apps.music.client

import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.usecases.ImportFromInternetUseCase
import dev.younesgouyd.apps.music.client.usecases.ImportLocalFileUseCase
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile

class ImportService(
    private val importSessionRepo: ImportSessionRepo,
    private val importSessionItemRepo: ImportSessionItemRepo,
    private val playlistRepo: PlaylistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val mediaFileRepo: MediaFileRepo,
    private val mediaFilePlaylistCrossRefRepo: MediaFilePlaylistCrossRefRepo,
    private val importLocalFileUseCase: ImportLocalFileUseCase,
    private val importFromInternetUseCase: ImportFromInternetUseCase
) {
    private val coroutineScope = Music.coroutineScope
    private var started: Boolean = false

    @Volatile
    private var currentSessionId: Long? = null

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
                if (currentSessionId == session.id) {
                    println("Session item ${session.id} was already started")
                    return@collect
                }
                importSessionItemRepo.updateState(session.id, ImportSessionItem.State.InProgress)
                currentSessionId = session.id
                val sessionState = importSessionItemRepo.get(session.id).map { it.state }
                launch {
                    sessionState.collect { state ->
                        when (state) {
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
                                            session = importSessionRepo.get(session.importSessionId).first(),
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
                                        importSessionItemRepo.updateState(session.id, ImportSessionItem.State.Failed)
                                        e.printStackTrace()
                                    }
                                }
                            }

                            ImportSessionItem.State.Completed -> {
                                println("Session item ${session.id} Completed → stopping")
                                currentSessionId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }

                            ImportSessionItem.State.Cancelled -> {
                                println("Session item ${session.id} Cancelled → stopping")
                                currentSessionId = null
                                importJob = null
                                cancel(message = "IntendedCancellation", cause = IntendedCancellation())
                            }

                            ImportSessionItem.State.Failed -> {
                                println("Session item ${session.id} Failed → stopping")
                                currentSessionId = null
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
        if (currentSessionId != null) {
            importSessionItemRepo.updateState(currentSessionId!!, ImportSessionItem.State.Failed)
        }
    }

    private suspend fun import(session: ImportSession, item: ImportSessionItem) {
        withContext(Dispatchers.IO) {
            println("Working on session ${session.id} item ${item.id}")
            val playlistName: String
            val containerImageMediaFileId: Long?
            val trackId = when (session.sourceType) {
                ImportSession.SourceType.Local -> {
                    playlistName = "from import ${session.id}"
                    containerImageMediaFileId = null
                    importLocalFileUseCase.execute(
                        inspection = item.inspection as Inspection.ItemInspection.LocalFileTrack, // TODO
                        importSessionItemId = item.id,
                        folderId = session.destinationFolderId
                    )
                }
                ImportSession.SourceType.Internet -> {
                    val container = (session.inspection as Inspection.ContainerInspection.Webpage)
                    playlistName = container.title ?: "from import ${session.id}"
                    containerImageMediaFileId = mediaFileRepo.getImportSessionImageMediaFile(session.id)?.id
                    importFromInternetUseCase.execute(
                        inspection = item.inspection as Inspection.ItemInspection.InternetTrack, // TODO
                        importSessionItemId = item.id,
                        folderId = session.destinationFolderId
                    )
                }
            }
            val state = if (trackId != null) ImportSessionItem.State.Completed else ImportSessionItem.State.Failed
            importSessionItemRepo.updateState(
                id = item.id,
                state = state
            )
            if (trackId != null) {
                val playlist = playlistRepo.getImportSessionPlaylist(session.id).first()
                val playlistId: Long
                if (playlist != null) {
                    playlistId = playlist.id
                } else {
                    playlistId = playlistRepo.add(
                        name = playlistName,
                        folderId = null,
                        importSessionId = session.id,
                        importUri = session.uri
                    )
                    if (containerImageMediaFileId != null) {
                        mediaFilePlaylistCrossRefRepo.add(
                            mediaFileId = containerImageMediaFileId,
                            playlistId = playlistId
                        )
                    }
                }
                playlistTrackCrossRefRepo.add(playlistId, trackId)
            }
        }
    }

    private class IntendedCancellation : Throwable(null, null, false, false) {
        private fun readResolve(): Any = IntendedCancellation() // TODO (this was recommended by ide)
    }
}