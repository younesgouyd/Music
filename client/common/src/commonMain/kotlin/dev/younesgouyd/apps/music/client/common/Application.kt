package dev.younesgouyd.apps.music.client.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.common.components.Main
import dev.younesgouyd.apps.music.client.common.components.SplashScreen
import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.client.common.data.FileManager
import dev.younesgouyd.apps.music.client.common.data.RepoStore
import dev.younesgouyd.apps.music.client.common.usecases.*
import dev.younesgouyd.apps.music.client.common.util.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*

class Application {
    companion object {
        private val logger = KotlinLogging.logger {}

        private var backHandlers: Stack<() -> Unit> = Stack()

        fun registerBackHandler(onBack: () -> Unit) {
            backHandlers.push(onBack)
        }

        fun unregisterLastBackHandler() {
            try {
                backHandlers.pop()
            } catch (e: EmptyStackException) {
                logger.warn(e) { }
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var mediaController: MediaController
    private lateinit var backend: Backend
    private lateinit var fileManager: FileManager
    private lateinit var repoStore: RepoStore

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private val loading = MutableStateFlow(false)
    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        SplashScreen(
            onStart = ::start,
            loading = loading.asStateFlow()
        )
    )

    private fun start(serverAddress: String) {
        coroutineScope.launch {
            val tempDir = withContext(Dispatchers.IO) {
                appDir.mkdir()
                File(appDir, "temp").also { it.mkdir() }
            }
            backend = Backend(serverAddress)
            fileManager = FileManager(tempDir)
            fileManager.clearTemp()
            repoStore = RepoStore(backend, fileManager)
            unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(backend)
            setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(backend)
            clearImportItemUseCase = ClearImportItemUseCase(backend)
            deleteFolderUseCase = DeleteFolderUseCase(backend)
            prepareImportUseCase = PrepareImportUseCase(backend)
            mediaController = MediaController(
                mediaPlayer = createMediaPlayer(),
                mediaFileRepo = repoStore.mediaFileRepo,
                trackRepo = repoStore.trackRepo,
                artistRepo = repoStore.spotifyArtistRepo,
                albumRepo = repoStore.spotifyAlbumRepo,
            )
            currentComponent.update {
                it.clear()
                Main(
                    repoStore = repoStore,
                    deleteFolderUseCase = deleteFolderUseCase,
                    clearImportItemUseCase = clearImportItemUseCase,
                    setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
                    unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                    prepareImportUseCase = prepareImportUseCase,
                    mediaController = mediaController,
                )
            }
        }
    }

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    fun clear() {
        try { currentComponent.value.clear() } catch (_: Exception) { }
        try { mediaController.release() } catch (_: Exception) { }
        try { runBlocking { backend.close() } } catch (_: Exception) { }
        try { coroutineScope.cancel() } catch (_: Exception) { }
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }
}
