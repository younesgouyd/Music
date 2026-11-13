package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.Application
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.multiplatform.usecases.*
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.ComponentId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Stack

class NavigationHost(
    private val toggleDrawerState: suspend () -> Unit,
    repoStore: RepoStore,
    deleteFolderUseCase: DeleteFolderUseCase,
    clearImportItemUseCase: ClearImportItemUseCase,
    setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
    unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    prepareImportUseCase: PrepareImportUseCase,
    mediaController: MediaController,
    startDestination: Destination
) : Component() {
    override val title: String = ""
    private val navController = NavigationController(
        repoStore = repoStore,
        deleteFolderUseCase = deleteFolderUseCase,
        clearImportItemUseCase = clearImportItemUseCase,
        setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
        unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
        prepareImportUseCase = prepareImportUseCase,
        mediaController = mediaController,
        startDestination = startDestination
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun show(modifier: Modifier) {
        val navController by rememberUpdatedState(navController)
        val currentDestination by navController.currentDestination.collectAsState()
        val inHome by navController.inHome.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val stateHolder = rememberSaveableStateHolder()

        LaunchedEffect(Unit) {
            navController.removed.collect {
                stateHolder.removeState(it)
            }
        }

        DisposableEffect(Unit) {
            Application.registerBackHandler {
                if (!inHome) {
                    navController.navigateBack()
                }
            }
            onDispose {
                Application.unregisterLastBackHandler()
            }
        }

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                content = { Icon(Icons.Default.Menu, null) },
                                onClick = {
                                    coroutineScope.launch { toggleDrawerState() }
                                }
                            )
                            if (!inHome) {
                                IconButton(
                                    content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) },
                                    onClick = navController::navigateBack
                                )
                            }
                        }
                    },
                    title = { Text(text = currentDestination.title, style = MaterialTheme.typography.headlineMedium) }
                )
            },
            content = { paddingValues ->
                stateHolder.SaveableStateProvider(currentDestination.id) {
                    currentDestination.show(Modifier.padding(paddingValues))
                }
            }
        )
    }

    override fun clear() {
        navController.dispose()
        coroutineScope.cancel()
    }

    fun navigateTo(destination: Destination) {
        navController.navigateTo(destination)
    }

    sealed class Destination {
        data object Library : Destination()

        data object PlaylistList : Destination()

        data class PlaylistDetails(val id: PlaylistId) : Destination()

        data object ArtistList : Destination()

        data class ArtistDetails(val id: SpotifyArtistId) : Destination()

        data class AlbumDetails(val id: SpotifyAlbumId) : Destination()

        data object TagList : Destination()

        data class TagDetails(val id: TagId) : Destination()

        data object TrackList : Destination()

        data class TrackDetails(val id: TrackId) : Destination()

        data object ImportList : Destination()

        data class ImportDetails(val id: ImportSessionId, val defaultTab: ImportSessionItem.State = ImportSessionItem.State.Completed) : Destination()

        data class ImportItemDetails(val id: ImportSessionItemId) : Destination()

        data class ImportFlow(val destinationFolderId: FolderId) : Destination()
    }

    private class NavigationController(
        private val repoStore: RepoStore,
        private val deleteFolderUseCase: DeleteFolderUseCase,
        private val clearImportItemUseCase: ClearImportItemUseCase,
        private val setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
        private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
        private val prepareImportUseCase: PrepareImportUseCase,
        private val mediaController: MediaController,
        startDestination: Destination
    ) {
        val inHome: StateFlow<Boolean>
        val currentDestination: StateFlow<Component>
        val removed: SharedFlow<ComponentId>
            field = MutableSharedFlow<ComponentId>(extraBufferCapacity = 100)

        private val destinationFactory: DestinationFactory = DestinationFactory()
        private val backStack: BackStack

        init {
            backStack = BackStack(destinationFactory.get(startDestination))
            inHome = backStack.inHome.asStateFlow()
            currentDestination = backStack.currentDestination.asStateFlow()
        }

        fun navigateTo(destination: Destination) {
            backStack.push(destinationFactory.get(destination))
        }

        fun replaceCurrentWith(destination: Destination) {
            val old = backStack.top()
            backStack.replace(destinationFactory.get(destination))
            old.clear()
            removed.tryEmit(old.id)
        }

        fun navigateBack() {
            val old = backStack.top()
            backStack.pop()
            old.clear()
            removed.tryEmit(old.id)
        }

        fun dispose() {
            while (backStack.isNotEmpty()) {
                backStack.top().clear()
                backStack.pop()
            }
        }

        private class BackStack(startDestination: Component) {
            val inHome: MutableStateFlow<Boolean>
            val currentDestination: MutableStateFlow<Component>
            private val stack: Stack<Component>

            init {
                stack = Stack<Component>().apply { push(startDestination) }
                currentDestination = MutableStateFlow(startDestination)
                inHome = MutableStateFlow(true)
            }

            fun push(component: Component) {
                stack.push(component)
                currentDestination.update { stack.peek() }
                inHome.update { stack.size == 1 }
            }

            fun replace(component: Component) {
                stack.pop()
                stack.push(component)
                currentDestination.update { stack.peek() }
                inHome.update { stack.size == 1 }
            }

            fun pop() {
                stack.pop()
                if (stack.isNotEmpty()) {
                    currentDestination.update { stack.peek() }
                }
                inHome.update { stack.size == 1 }
            }

            fun top(): Component {
                return stack.peek()
            }

            fun isNotEmpty(): Boolean {
                return stack.isNotEmpty()
            }
        }

        private inner class DestinationFactory {
            fun get(destination: Destination): Component {
                return when (destination) {
                    is Destination.Library -> Library(
                        tagRepo = repoStore.tagRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        trackRepo = repoStore.trackRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        deleteFolderUseCase = deleteFolderUseCase,
                        clearImportItemUseCase = clearImportItemUseCase,
                        mediaController = mediaController,
                        showImportFlow = { navigateTo(Destination.ImportFlow(it)) },
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        albumRepo = repoStore.spotifyAlbumRepo
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.id,
                        artistRepo = repoStore.spotifyArtistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        albumRepo = repoStore.spotifyAlbumRepo,
                        showAlbum = { navigateTo(Destination.AlbumDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.AlbumDetails -> AlbumDetails(
                        id = destination.id,
                        albumRepo = repoStore.spotifyAlbumRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        spotifyTrackRepo = repoStore.spotifyTrackRepo,
                        mediaController = mediaController,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistList -> ArtistList(
                        artistRepo = repoStore.spotifyArtistRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.PlaylistDetails -> PlaylistDetails(
                        id = destination.id,
                        trackRepo = repoStore.trackRepo,
                        playlistRepo = repoStore.playlistRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        folderRepo = repoStore.folderRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        albumRepo = repoStore.spotifyAlbumRepo,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.PlaylistList -> PlaylistList(
                        playlistRepo = repoStore.playlistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        mediaController = mediaController,
                        showPlaylistDetails = { navigateTo(Destination.PlaylistDetails(it)) },
                        albumRepo = repoStore.spotifyAlbumRepo
                    )
                    is Destination.ImportList -> ImportList(
                        importSessionRepo = repoStore.importSessionRepo,
                        showImportDetails = { navigateTo(Destination.ImportDetails(it)) }
                    )
                    is Destination.ImportDetails -> ImportDetails(
                        id = destination.id,
                        defaultTab = destination.defaultTab,
                        importSessionRepo = repoStore.importSessionRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        clearImportItemUseCase = clearImportItemUseCase,
                        showImportItem = { navigateTo(Destination.ImportItemDetails(it)) }
                    )
                    is Destination.TagList -> TagList(
                        tagRepo = repoStore.tagRepo,
                        showTag = { navigateTo(Destination.TagDetails(it)) }
                    )
                    is Destination.TagDetails -> TagDetails(
                        id = destination.id,
                        tagRepo = repoStore.tagRepo,
                        trackRepo = repoStore.trackRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.TrackList -> TrackList(
                        trackRepo = repoStore.trackRepo,
                        tagRepo = repoStore.tagRepo,
                        mediaController = mediaController,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.TrackDetails -> TrackDetails(
                        id = destination.id,
                        trackRepo = repoStore.trackRepo,
                        tagRepo = repoStore.tagRepo,
                        spotifyTrackRepo = repoStore.spotifyTrackRepo,
                        spotifySearchRepo = repoStore.spotifySearchRepo,
                        playlistRepo = repoStore.playlistRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
                        unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                        mediaController = mediaController,
                        showTag = { navigateTo(Destination.TagDetails(it)) },
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showImportSessionItem = { navigateTo(Destination.ImportItemDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showAlbum = { navigateTo(Destination.AlbumDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ImportItemDetails -> ImportItemDetails(
                        id = destination.id,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        importSessionRepo = repoStore.importSessionRepo,
                        trackRepo = repoStore.trackRepo,
                        showImportSession = { navigateTo(Destination.ImportDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.ImportFlow -> ImportFlow(
                        destinationFolderId = destination.destinationFolderId,
                        inspectionRepo = repoStore.inspectionRepo,
                        prepareImportUseCase = prepareImportUseCase,
                        showImportSession = { id, tab -> replaceCurrentWith(Destination.ImportDetails(id, tab)) }
                    )
                }
            }
        }
    }
}
