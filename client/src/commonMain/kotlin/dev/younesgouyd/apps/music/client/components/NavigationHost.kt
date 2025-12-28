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

        data class ArtistDetails(val id: ArtistId) : Destination()

        data object TagList : Destination()

        data class TagDetails(val id: TagId) : Destination()

        data object TrackList : Destination()

        data class TrackDetails(val id: TrackId) : Destination()

        data object ImportList : Destination()

        data class ImportDetails(val id: ImportSessionId) : Destination()

        data class ImportItemDetails(val id: ImportSessionItemId) : Destination()

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
                        server = repoStore.server,
                        tagRepo = repoStore.tagRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        trackRepo = repoStore.trackRepo,
                        artistRepo = repoStore.artistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        importSessionWithItemsRepo = repoStore.importSessionWithItemsRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaFileImportSessionCrossRefRepo = repoStore.mediaFileImportSessionCrossRefRepo,
                        mediaFileImportSessionItemCrossRefRepo = repoStore.mediaFileImportSessionItemCrossRefRepo,
                        mediaController = mediaController,
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) }
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.id,
                        artistRepo = repoStore.artistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistList -> ArtistList(
                        artistRepo = repoStore.artistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.PlaylistDetails -> PlaylistDetails(
                        id = destination.id,
                        trackRepo = repoStore.trackRepo,
                        playlistRepo = repoStore.playlistRepo,
                        artistRepo = repoStore.artistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        playlistTrackViewRepo = repoStore.playlistTrackViewRepo,
                        folderRepo = repoStore.folderRepo,
                        mediaController = mediaController,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        showImport = { navigateTo(Destination.ImportDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.PlaylistList -> PlaylistList(
                        playlistRepo = repoStore.playlistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        artistRepo = repoStore.artistRepo,
                        mediaController = mediaController,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        showPlaylistDetails = { navigateTo(Destination.PlaylistDetails(it)) }
                    )
                    is Destination.ImportList -> ImportList(
                        importSessionRepo = repoStore.importSessionRepo,
                        showImportDetails = { navigateTo(Destination.ImportDetails(it)) }
                    )
                    is Destination.ImportDetails -> ImportDetails(
                        id = destination.id,
                        importSessionRepo = repoStore.importSessionRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
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
                        artistRepo = repoStore.artistRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.TrackList -> TrackList(
                        trackRepo = repoStore.trackRepo,
                        tagRepo = repoStore.tagRepo,
                        artistRepo = repoStore.artistRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        mediaController = mediaController,
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.TrackDetails -> TrackDetails(
                        id = destination.id,
                        trackRepo = repoStore.trackRepo,
                        tagRepo = repoStore.tagRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        artistRepo = repoStore.artistRepo,
                        playlistRepo = repoStore.playlistRepo,
                        tagTrackCrossRefRepo = repoStore.tagTrackCrossRefRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) },
                        showTag = { navigateTo(Destination.TagDetails(it)) },
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showImportSessionItem = { navigateTo(Destination.ImportItemDetails(it)) }
                    )
                    is Destination.ImportItemDetails -> ImportItemDetails(
                        id = destination.id,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        importSessionRepo = repoStore.importSessionRepo,
                        trackRepo = repoStore.trackRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                        artistRepo = repoStore.artistRepo,
                        showImportSession = { navigateTo(Destination.ImportDetails(it)) },
                        showTrack = { navigateTo(Destination.TrackDetails(it)) },
                        showArtist = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                }
            }
        }
    }
}
