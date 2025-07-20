package dev.younesgouyd.apps.music.desktop.components

import androidx.compose.foundation.clickable
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
import dev.younesgouyd.apps.music.desktop.components.util.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class Main(private val repoStore: RepoStore) : Main(repoStore) {
    override val mediaController = MediaController(repoStore)

    override val navigationHost: MutableStateFlow<NavigationHost> = MutableStateFlow(
        NavigationHost(
            repoStore = repoStore,
            mediaController = mediaController,
            startDestination = NavigationHost.Destination.Library,
            toggleDrawerState = ::toggleDrawerState
        )
    )
    override val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val playerExpanded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showAlbumDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.AlbumDetails(it))
        },
        showArtistDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.ArtistDetails(it))
        }
    )
    override val player: Component = Player(
        mediaController = mediaController,
        showAlbumDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.AlbumDetails(it))
            playerExpanded.value = false
        },
        showArtistDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            playerExpanded.value = false
        },
        minimizePlayer = { playerExpanded.value = false }
    )
    val queue = Queue(mediaController)

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            modifier = modifier,
            darkTheme = darkTheme,
            navigationHost = navigationHost.asStateFlow(),
            player = player,
            miniPlayer = miniPlayer,
            playerExpanded = playerExpanded.asStateFlow(),
            queue = queue,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
            drawerState = drawerState.asStateFlow(),
            onExpandPlayerClick = { playerExpanded.value = true },
            onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
        )
    }

    private fun handleNavigationDrawerItemClick(item: NavigationDrawerItems) {
        navigationHost.update {
            it.clear()
            NavigationHost(
                repoStore = repoStore,
                mediaController = mediaController,
                startDestination = when (item) {
                    NavigationDrawerItems.Settings -> NavigationHost.Destination.Settings
                    NavigationDrawerItems.Library -> NavigationHost.Destination.Library
                    NavigationDrawerItems.Playlists -> NavigationHost.Destination.PlaylistList
                    NavigationDrawerItems.Albums -> NavigationHost.Destination.AlbumList
                    NavigationDrawerItems.Artists -> NavigationHost.Destination.ArtistList
                },
                toggleDrawerState = ::toggleDrawerState
            )
        }
        selectedNavigationDrawerItem.value = item
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            darkTheme: DarkThemeOptions,
            navigationHost: StateFlow<Component>,
            player: Component,
            miniPlayer: Component,
            queue: Component,
            onExpandPlayerClick: () -> Unit,
            playerExpanded: StateFlow<Boolean>,
            selectedNavigationDrawerItem: StateFlow<NavigationDrawerItems>,
            drawerState: StateFlow<DrawerState>,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val navigationHost by navigationHost.collectAsState()
            val drawerState by drawerState.collectAsState()
            val playerExpanded by playerExpanded.collectAsState()
            val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()

            YounesMusicTheme(
                darkTheme = darkTheme,
                content = {
                    Surface(
                        modifier = modifier,
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(weight = .8f)
                                        ) {
                                            navigationHost.show(Modifier.weight(.7f))
                                            queue.show(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp).weight(.3f))
                                        }
                                        miniPlayer.show(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(8.dp)
                                                .weight(.2f)
                                                .clickable { onExpandPlayerClick() }
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