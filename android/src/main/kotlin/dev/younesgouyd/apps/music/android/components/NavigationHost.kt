package dev.younesgouyd.apps.music.android.components

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
import dev.younesgouyd.apps.music.common.components.NavigationHost
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class NavigationHost(
    repoStore: RepoStore,
    mediaController: MediaController,
    startDestination: Destination,
    toggleDrawerState: suspend () -> Unit
) : NavigationHost(NavigationController(repoStore, mediaController, startDestination), toggleDrawerState) {
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
                                    onClick = { navController.navigateBack() }
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

    private class NavigationController(
        private val repoStore: RepoStore,
        private val mediaController: MediaController,
        startDestination: Destination
    ) : NavigationHost.NavigationController() {
        override val inHome: StateFlow<Boolean>
        override val currentDestination: StateFlow<Component>

        private val destinationFactory: DestinationFactory = DestinationFactory()
        private val backStack: BackStack


        init {
            backStack = BackStack(destinationFactory.get(startDestination))
            inHome = backStack.inHome.asStateFlow()
            currentDestination = backStack.currentDestination.asStateFlow()
        }

        override fun navigateTo(destination: Destination) {
            backStack.push(destinationFactory.get(destination))
        }

        override fun navigateBack() {
            backStack.top().clear()
            backStack.pop()
        }

        override fun dispose() {
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
                    is Destination.Settings -> Settings(repoStore)
                    is Destination.Library -> Library(
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        trackRepo = repoStore.trackRepo,
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        artistTrackCrossRefRepo = repoStore.artistTrackCrossRefRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        mediaController = mediaController,
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.AlbumDetails -> AlbumDetails(
                        id = destination.albumId,
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        trackRepo = repoStore.trackRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        playlistRepo = repoStore.playlistRepo,
                        folderRepo = repoStore.folderRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.AlbumList -> AlbumList(
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        playlistRepo = repoStore.playlistRepo,
                        folderRepo = repoStore.folderRepo,
                        mediaController = mediaController,
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.artistId,
                        artistRepo = repoStore.artistRepo,
                        albumRepo = repoStore.albumRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        mediaController = mediaController,
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.ArtistList -> ArtistList(
                        artistRepo = repoStore.artistRepo,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) }
                    )
                    is Destination.PlaylistDetails -> PlaylistDetails(
                        id = destination.playlistId,
                        trackRepo = repoStore.trackRepo,
                        playlistRepo = repoStore.playlistRepo,
                        artistRepo = repoStore.artistRepo,
                        albumRepo = repoStore.albumRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        folderRepo = repoStore.folderRepo,
                        mediaController = mediaController,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) }
                    )
                    is Destination.PlaylistList -> PlaylistList(
                        playlistRepo = repoStore.playlistRepo,
                        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
                        trackRepo = repoStore.trackRepo,
                        folderRepo = repoStore.folderRepo,
                        albumRepo = repoStore.albumRepo,
                        mediaController = mediaController,
                        showPlaylistDetails = { navigateTo(Destination.PlaylistDetails(it)) }
                    )
                }
            }
        }
    }
}
