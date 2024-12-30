package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.DarkThemeOptions
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.data.RepoStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Main(
    private val repoStore: RepoStore
) : Component() {
    override val title: String = ""
    private val mainComponentController = MainComponentController()
    private val darkTheme: StateFlow<DarkThemeOptions> = repoStore.settingsRepo.getDarkThemeFlow().filterNotNull().stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = DarkThemeOptions.SystemDefault
    )

    private val mediaController = MediaController(
        coroutineScope = coroutineScope,
        trackRepo = repoStore.trackRepo,
        artistRepo = repoStore.artistRepo,
        albumRepo = repoStore.albumRepo,
        playlistRepo = repoStore.playlistRepo,
        onAlbumClick = mainComponentController::showAlbums,
        onArtistClick = mainComponentController::showArtists
    )
    private val player = Player(mediaController)
    private val queue = Queue(mediaController)

    private val settingsHost: Settings by lazy { Settings(repoStore) }
    private val libraryHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.Library) }
    private val playlistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.PlaylistList) }
    private val artistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.ArtistList) }
    private val albumsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.AlbumList) }

    private val currentMainComponent: MutableStateFlow<Component> = MutableStateFlow(albumsHost)
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Albums)

    @Composable
    override fun show(modifier: Modifier) {
        val currentMainComponent by currentMainComponent.collectAsState()
        val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            darkTheme = darkTheme,
            currentMainComponent = currentMainComponent,
            player = player,
            queue = queue,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem,
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

    override fun clear() {
        mediaController.release()
        settingsHost.clear()
        libraryHost.clear()
        coroutineScope.cancel()
    }

    private inner class MainComponentController {
        fun showSettings() {
            currentMainComponent.update { settingsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Settings }
        }

        fun showLibrary() {
            currentMainComponent.update { libraryHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Library }
        }

        fun showPlaylists(id: Long?) {
            currentMainComponent.update { playlistsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Playlists }
            if (id != null) { artistsHost.navigateTo(NavigationHost.Destination.PlaylistDetails(id)) }
        }

        fun showAlbums(id: Long?) {
            currentMainComponent.update { albumsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Albums }
            if (id != null) { artistsHost.navigateTo(NavigationHost.Destination.AlbumDetails(id)) }
        }

        fun showArtists(id: Long?) {
            currentMainComponent.update { artistsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Artists }
            if (id != null) { artistsHost.navigateTo(NavigationHost.Destination.ArtistDetails(id)) }
        }
    }

    private enum class NavigationDrawerItems(val label: String) {
        Settings("Settings"),
        Library("Library"),
        Playlists("Playlists"),
        Albums("Albums"),
        Artists("Artists")
    }

    private object Ui {
        @Composable
        fun Main(
            darkTheme: DarkThemeOptions,
            currentMainComponent: Component,
            player: Component,
            queue: Component,
            selectedNavigationDrawerItem: NavigationDrawerItems,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            YounesMusicTheme(
                darkTheme = darkTheme,
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            DismissibleNavigationDrawer(
                                modifier = Modifier.fillMaxWidth().weight(weight = .8f),
                                drawerContent = {
                                    DismissibleDrawerSheet {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        currentMainComponent.show(Modifier.weight(.7f))
                                        queue.show(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp).weight(.3f))
                                    }
                                }
                            )
                            player.show(Modifier.padding(8.dp).weight(.2f))
                        }
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