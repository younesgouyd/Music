package dev.younesgouyd.apps.music.android.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.android.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.Main
import dev.younesgouyd.apps.music.common.components.NavigationHost
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Main(
    repoStore: RepoStore,
    media3Controller: androidx.media3.session.MediaController
) : Main(repoStore) {
    override val mediaController = MediaController(media3Controller, repoStore)

    override val settingsHost: Settings by lazy { Settings(repoStore) }
    override val libraryHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.Library, ::toggleDrawerState) }
    override val playlistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.PlaylistList, ::toggleDrawerState) }
    override val artistsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.ArtistList, ::toggleDrawerState) }
    override val albumsHost: NavigationHost by lazy { NavigationHost(repoStore, mediaController, NavigationHost.Destination.AlbumList, ::toggleDrawerState) }

    override val currentMainComponent: MutableStateFlow<Component> = MutableStateFlow(libraryHost)
    override val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val playerExpanded: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showAlbumDetails = {}, // TODO
        showArtistDetails = {} // TODO
    )
    override val player: Component = Player(
        mediaController = mediaController,
        showAlbumDetails = {
            albumsHost.navigateTo(NavigationHost.Destination.AlbumDetails(it))
            currentMainComponent.value = albumsHost
            playerExpanded.value = false
        },
        showArtistDetails = {
            artistsHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            currentMainComponent.value = artistsHost
            playerExpanded.value = false
        },
        minimizePlayer = { playerExpanded.value = false }
    )

    @Composable
    override fun show(modifier: Modifier) {
        val currentMainComponent by currentMainComponent.collectAsState()
        val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            modifier = modifier,
            darkTheme = darkTheme,
            currentMainComponent = currentMainComponent,
            player = player,
            miniPlayer = miniPlayer,
            playerExpanded = playerExpanded.asStateFlow(),
            selectedNavigationDrawerItem = selectedNavigationDrawerItem,
            drawerState = drawerState.asStateFlow(),
            onExpandPlayerClick = { playerExpanded.value = true },
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
            modifier: Modifier,
            darkTheme: DarkThemeOptions,
            currentMainComponent: Component,
            player: Component,
            miniPlayer: Component,
            onExpandPlayerClick: () -> Unit,
            playerExpanded: StateFlow<Boolean>,
            selectedNavigationDrawerItem: NavigationDrawerItems,
            drawerState: StateFlow<DrawerState>,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val drawerState by drawerState.collectAsState()
            val playerExpanded by playerExpanded.collectAsState()

            YounesMusicTheme(
                darkTheme = darkTheme,
                content = {
                    Surface(
                        modifier = modifier.fillMaxSize(),
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
                                    if (playerExpanded) {
                                        player.show(Modifier.fillMaxSize())
                                    } else {
                                        currentMainComponent.show(Modifier.fillMaxWidth().weight(weight = 0.88f))
                                        miniPlayer.show(
                                            modifier = Modifier.clickable { onExpandPlayerClick() }
                                                .fillMaxWidth()
                                                .weight(0.12f)
                                        )
                                    }
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