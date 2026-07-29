package dev.younesgouyd.apps.music.client.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.common.components.Main
import dev.younesgouyd.apps.music.client.common.components.SplashScreen
import dev.younesgouyd.apps.music.client.common.data.RepoStore
import dev.younesgouyd.apps.music.client.common.usecases.*
import dev.younesgouyd.apps.music.client.common.util.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

    private lateinit var repoStore: RepoStore

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(SplashScreen())

    fun start() {
        coroutineScope.launch {
            System.setProperty("sun.java2d.uiScale", "1.0")
            repoStore = RepoStore()
            unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(repoStore.client)
            setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(repoStore.client)
            clearImportItemUseCase = ClearImportItemUseCase(repoStore.client)
            deleteFolderUseCase = DeleteFolderUseCase(repoStore.client)
            prepareImportUseCase = PrepareImportUseCase(repoStore.client)
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
        currentComponent.value.clear()
        mediaController.release()
        repoStore.release()
        coroutineScope.cancel()
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }
}
