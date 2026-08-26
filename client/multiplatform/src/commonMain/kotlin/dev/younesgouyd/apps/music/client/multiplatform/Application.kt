package dev.younesgouyd.apps.music.client.multiplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.multiplatform.components.Main
import dev.younesgouyd.apps.music.client.multiplatform.components.StartupScreen
import dev.younesgouyd.apps.music.client.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.client.multiplatform.usecases.*
import dev.younesgouyd.apps.music.client.multiplatform.util.Component
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
    private lateinit var server: Server
    private lateinit var repoStore: RepoStore

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private val loading = MutableStateFlow(false)
    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        StartupScreen(
            onStart = ::start,
            loading = loading.asStateFlow()
        )
    )

    private fun start(
        serverIpAddr: String,
        serverFingerprint: String,
        password: String
    ) {
        coroutineScope.launch {
            val tempDir = withContext(Dispatchers.IO) {
                appDir.mkdir()
                val dir = File(appDir, "temp")
                if (dir.exists()) {
                    dir.delete()
                }
                dir.mkdir()
                dir
            }
            server = Server(serverIpAddr, serverFingerprint, password,tempDir)
            repoStore = RepoStore(server)
            unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(server)
            setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(server)
            clearImportItemUseCase = ClearImportItemUseCase(server)
            deleteFolderUseCase = DeleteFolderUseCase(server)
            prepareImportUseCase = PrepareImportUseCase(server)
            mediaController = MediaController(
                mediaPlayer = createMediaPlayer(),
                mediaFileRepo = repoStore.mediaFileRepo,
                trackRepo = repoStore.trackRepo,
                artistRepo = repoStore.spotifyArtistRepo,
                albumRepo = repoStore.spotifyAlbumRepo,
            )
            server.connect()
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
        try { runBlocking { server.close() } } catch (_: Exception) { }
        try { coroutineScope.cancel() } catch (_: Exception) { }
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }
}
