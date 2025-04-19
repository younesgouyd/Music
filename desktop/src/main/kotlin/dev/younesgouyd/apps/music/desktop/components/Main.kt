package dev.younesgouyd.apps.music.desktop.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.components.Main
import dev.younesgouyd.apps.music.common.components.NavigationHost
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import dev.younesgouyd.apps.music.common.util.MediaPlayer
import dev.younesgouyd.apps.music.common.util.MediaUtil
import dev.younesgouyd.apps.music.desktop.components.util.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Main(
    repoStore: RepoStore,
    mediaPlayer: MediaPlayer,
    mediaUtil: MediaUtil
) : Main(repoStore, mediaPlayer, mediaUtil) {
    override val mediaController = MediaController(
        trackRepo = repoStore.trackRepo,
        artistRepo = repoStore.artistRepo,
        albumRepo = repoStore.albumRepo,
        playlistRepo = repoStore.playlistRepo,
        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
        folderRepo = repoStore.folderRepo,
        onAlbumClick = mainComponentController::showAlbums,
        onArtistClick = mainComponentController::showArtists,
        mediaPlayer = mediaPlayer,
        mediaUtil = mediaUtil
    )

    override val miniPlayer = Player(mediaController)
    override val queue = Queue(mediaController)

    override val settingsHost: Settings by lazy { Settings(repoStore) }
    override val libraryHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.Library, ::toggleDrawerState) }
    override val playlistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.PlaylistList, ::toggleDrawerState) }
    override val artistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.ArtistList, ::toggleDrawerState) }
    override val albumsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.AlbumList, ::toggleDrawerState) }

    override val currentMainComponent: MutableStateFlow<Component> = MutableStateFlow(libraryHost)
    override val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    @Composable
    override fun show(modifier: Modifier) {
        val currentMainComponent by currentMainComponent.collectAsState()
        val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            darkTheme = darkTheme,
            currentMainComponent = currentMainComponent,
            miniPlayer = miniPlayer,
            queue = queue,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem,
            drawerState = drawerState.asStateFlow(),
            onNavigationDrawerItemClick = {
                when (it) {
                    NavigationDrawerItems.Settings -> mainComponentController.showSettings()
                    NavigationDrawerItems.Library -> mainComponentController.showLibrary()
                    NavigationDrawerItems.Playlists -> mainComponentController.showPlaylists(null)
                    NavigationDrawerItems.Albums -> mainComponentController.showAlbums(null)
                    NavigationDrawerItems.Artists -> mainComponentController.showArtists(null)
                }
            }
        )
    }

    private object Ui {
        @Composable
        fun Main(
            darkTheme: DarkThemeOptions,
            currentMainComponent: Component,
            miniPlayer: Player,
            queue: Component,
            selectedNavigationDrawerItem: NavigationDrawerItems,
            drawerState: StateFlow<DrawerState>,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val drawerState by drawerState.collectAsState()

            YounesMusicTheme(
                darkTheme = darkTheme,
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        NavigationDrawerItems.entries.forEach {
                                            NavigationDrawerItem(
                                                label = { Text(it.label) },
                                                selected = it == selectedNavigationDrawerItem,
                                                onClick = { onNavigationDrawerItemClick(it) }
                                            )
                                        }
                                    }
                                }
                            },
                            content = {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(weight = .8f)
                                    ) {
                                        currentMainComponent.show(Modifier.weight(.7f))
                                        queue.show(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp).weight(.3f))
                                    }
                                    miniPlayer.show(Modifier.fillMaxWidth().padding(8.dp).weight(.2f))
                                }
                            }
                        )
                    }
                }
            )
        }

        @Composable
        fun YounesMusicTheme(
            darkTheme: DarkThemeOptions = DarkThemeOptions.SystemDefault,
            content: @Composable () -> Unit
        ) {
            MaterialTheme(
                colorScheme = when (darkTheme) {
                    DarkThemeOptions.SystemDefault -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                    DarkThemeOptions.Disabled -> lightColorScheme()
                    DarkThemeOptions.Enabled -> darkColorScheme()
                },
                content = content
            )
        }
    }
}