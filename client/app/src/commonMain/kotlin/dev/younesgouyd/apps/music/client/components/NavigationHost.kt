package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.usecases.ExportUseCaseImpl
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class NavigationHost(
    private val toggleDrawerState: suspend () -> Unit,
    repoStore: RepoStore,
    mediaController: MediaController,
    startDestination: Destination
) : Component() {
    override val title: String = ""
    private val navController = NavigationController(repoStore, mediaController, startDestination)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun show(modifier: Modifier) {
        val currentDestination by navController.currentDestination.collectAsState()
        val inHome by navController.inHome.collectAsState()
        val coroutineScope = rememberCoroutineScope()

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
                currentDestination.show(Modifier.padding(paddingValues))
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

        data class ImportFolderFlow(val destinationFolderId: FolderId) : Destination()

        data class ImportFromInternetFlow(val destinationFolderId: FolderId) : Destination()

        data object Export : Destination()
    }

    private class NavigationController(
        private val repoStore: RepoStore,
        private val mediaController: MediaController,
        startDestination: Destination
    ) {
        val inHome: StateFlow<Boolean>
        val currentDestination: StateFlow<Component>

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
            backStack.replace(destinationFactory.get(destination))
        }

        fun navigateBack() {
            backStack.top().clear()
            backStack.pop()
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
                inHome.update { false }
            }

            fun replace(component: Component) {
                stack.pop()
                stack.push(component)
                currentDestination.update { stack.peek() }
                inHome.update { false }
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
                        mediaFileRepo = repoStore.mediaFileRepo,
                        deleteFolderUseCase = repoStore.deleteFolderUseCase,
                        clearImportItemUseCase = repoStore.clearImportItemUseCase,
                        mediaController = mediaController,
                        showImportFolderFlow = { navigateTo(Destination.ImportFolderFlow(it)) },
                        showImportFromInternetFlow = { navigateTo(Destination.ImportFromInternetFlow(it)) },
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        albumRepo = repoStore.spotifyAlbumRepo
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.id,
                        artistRepo = repoStore.spotifyArtistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        albumRepo = repoStore.spotifyAlbumRepo,
                        showAlbum = { navigateTo(Destination.AlbumDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.AlbumDetails -> AlbumDetails(
                        id = destination.id,
                        albumRepo = repoStore.spotifyAlbumRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        spotifyTrackRepo = repoStore.spotifyTrackRepo,
                        mediaController = mediaController,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistList -> ArtistList(
                        artistRepo = repoStore.spotifyArtistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
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
                        mediaFileRepo = repoStore.mediaFileRepo,
                        showImport = { navigateTo(Destination.ImportDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        importSessionItemRepo = repoStore.importSessionItemRepo,
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
                        mediaFileRepo = repoStore.mediaFileRepo,
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
                        mediaFileRepo = repoStore.mediaFileRepo,
                        clearImportItemUseCase = repoStore.clearImportItemUseCase,
                        showImportItem = { navigateTo(Destination.ImportItemDetails(it)) }
                    )
                    is Destination.Export -> Export(
                        exportUseCase = ExportUseCaseImpl(repoStore)
                    )
                    is Destination.TagList -> TagList(
                        tagRepo = repoStore.tagRepo,
                        showTag = { navigateTo(Destination.TagDetails(it)) }
                    )
                    is Destination.TagDetails -> TagDetails(
                        id = destination.id,
                        tagRepo = repoStore.tagRepo,
                        trackRepo = repoStore.trackRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) },
                        importSessionItemRepo = repoStore.importSessionItemRepo
                    )
                    is Destination.TrackList -> TrackList(
                        trackRepo = repoStore.trackRepo,
                        tagRepo = repoStore.tagRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaController = mediaController,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) },
                        importSessionItemRepo = repoStore.importSessionItemRepo
                    )
                    is Destination.TrackDetails -> TrackDetails(
                        id = destination.id,
                        trackRepo = repoStore.trackRepo,
                        albumRepo = repoStore.spotifyAlbumRepo,
                        tagRepo = repoStore.tagRepo,
                        spotifyTrackRepo = repoStore.spotifyTrackRepo,
                        artistRepo = repoStore.spotifyArtistRepo,
                        playlistRepo = repoStore.playlistRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        setTrackMetadataFromSpotifyUseCase = repoStore.setTrackMetadataFromSpotifyUseCase,
                        unsetSpotifyTrackUseCase = repoStore.unsetSpotifyTrackUseCase,
                        mediaController = mediaController,
                        spotifyApi = repoStore.spotifyApi,
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
                        mediaFileRepo = repoStore.mediaFileRepo,
                        showImportSession = { navigateTo(Destination.ImportDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.ImportFolderFlow -> ImportFolderFlow(
                        destinationFolderId = destination.destinationFolderId,
                        folderRepo = repoStore.folderRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        importSessionRepo = repoStore.importSessionRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        showImportSession = { id, tab -> replaceCurrentWith(Destination.ImportDetails(id, tab)) }
                    )
                    is Destination.ImportFromInternetFlow -> ImportFromInternetFlow(
                        destinationFolderId = destination.destinationFolderId,
                        importSessionRepo = repoStore.importSessionRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        server = repoStore.server,
                        fileManager = repoStore.fileManager,
                        showImportSession = { id, tab -> replaceCurrentWith(Destination.ImportDetails(id, tab)) }
                    )
                }
            }
        }
    }
}
