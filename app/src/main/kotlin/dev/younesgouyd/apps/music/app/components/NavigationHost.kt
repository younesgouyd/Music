package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.data.RepoStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*

class NavigationHost(
    repoStore: RepoStore,
    mediaController: MediaController,
    startDestination: Destination
) : Component() {
    override val title: String = ""

    private val navController: NavigationController = NavigationController(
        repoStore = repoStore,
        mediaController = mediaController,
        startDestination = startDestination
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun show(modifier: Modifier) {
        val currentDestination by navController.currentDestination.collectAsState()
        val inHome by navController.inHome.collectAsState()

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (!inHome) {
                            IconButton(
                                content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) },
                                onClick = { navController.navigateBack() }
                            )
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

        data class PlaylistDetails(val playlistId: Long): Destination()

        data object ArtistList : Destination()

        data class ArtistDetails(val artistId: Long) : Destination()

        data object AlbumList : Destination()

        data class AlbumDetails(val albumId: Long) : Destination()
    }

    private class NavigationController(
        private val repoStore: RepoStore,
        private val mediaController: MediaController,
        startDestination: Destination
    ) {
        private val backStack: BackStack
        private val destinationFactory: DestinationFactory

        val inHome: StateFlow<Boolean>
            get() = backStack.inHome.asStateFlow()
        val currentDestination: StateFlow<Component>
            get() = backStack.currentDestination.asStateFlow()

        init {
            destinationFactory = DestinationFactory()
            backStack = BackStack(destinationFactory.get(startDestination))
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
                        folderRepo = repoStore.folderRepo,
                        playlistRepo = repoStore.playlistRepo,
                        trackRepo = repoStore.trackRepo,
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        artistTrackCrossRefRepo = repoStore.artistTrackCrossRefRepo,
                        addTrackToQueue = { mediaController.addToQueue(MediaController.QueueItemParameter.Track(it)) },
                        showPlaylist = { navigateTo(Destination.PlaylistDetails(it)) },
                        playTrack = { mediaController.play(listOf(MediaController.QueueItemParameter.Track(it))) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        playQueue = mediaController::play
                    )
                    is Destination.AlbumDetails -> AlbumDetails(
                        id = destination.albumId,
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        trackRepo = repoStore.trackRepo,
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        play = { mediaController.play(listOf(MediaController.QueueItemParameter.Album(destination.albumId))) },
                        addToQueueClick = { mediaController.addToQueue(MediaController.QueueItemParameter.Album(destination.albumId)) },
                        playTrack = { mediaController.play(listOf(MediaController.QueueItemParameter.Track(it))) }
                    )
                    is Destination.AlbumList -> AlbumList(
                        albumRepo = repoStore.albumRepo,
                        artistRepo = repoStore.artistRepo,
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        playAlbum = { mediaController.play(listOf(MediaController.QueueItemParameter.Album(it))) },
                        addAlbumToQueue = { mediaController.addToQueue(MediaController.QueueItemParameter.Album(it)) }
                    )
                    is Destination.ArtistDetails -> ArtistDetails(
                        id = destination.artistId,
                        artistRepo = repoStore.artistRepo,
                        albumRepo = repoStore.albumRepo,
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) },
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        playAlbum = { mediaController.play(listOf(MediaController.QueueItemParameter.Album(it))) }
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
                        showArtistDetails = { navigateTo(Destination.ArtistDetails(it)) },
                        showAlbumDetails = { navigateTo(Destination.AlbumDetails(it)) },
                        play = { mediaController.play(listOf(MediaController.QueueItemParameter.Playlist(destination.playlistId))) },
                        playTrack = { mediaController.play(listOf(MediaController.QueueItemParameter.Track(it))) }
                    )
                    is Destination.PlaylistList -> PlaylistList(
                        playlistRepo = repoStore.playlistRepo,
                        showPlaylistDetails = { navigateTo(Destination.PlaylistDetails(it)) },
                        playPlaylist = { mediaController.play(listOf(MediaController.QueueItemParameter.Playlist(it))) }
                    )
                }
            }
        }
    }
}
