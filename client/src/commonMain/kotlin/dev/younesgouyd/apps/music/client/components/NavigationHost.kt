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
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.RepoStore
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

        data class PlaylistDetails(val playlistId: PlaylistId) : Destination()

        data object ArtistList : Destination()

        data class ArtistDetails(val artistId: ArtistId) : Destination()

        data object ImportList : Destination()

        data class ImportDetails(val importId: ImportSessionId) : Destination()
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
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.artistId,
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
                        id = destination.playlistId,
                        trackRepo = repoStore.trackRepo,
                        playlistRepo = repoStore.playlistRepo,
                        artistRepo = repoStore.artistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
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
                        id = destination.importId,
                        importSessionRepo = repoStore.importSessionRepo,
                        importSessionItemRepo = repoStore.importSessionItemRepo,
                        mediaFileRepo = repoStore.mediaFileRepo,
                    )
                }
            }
        }
    }
}
