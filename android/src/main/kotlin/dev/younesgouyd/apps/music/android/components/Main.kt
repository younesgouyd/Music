package dev.younesgouyd.apps.music.android.components

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
import kotlinx.coroutines.flow.update

class Main(
    private val repoStore: RepoStore,
    media3Controller: androidx.media3.session.MediaController
) : Main(repoStore) {
    override val mediaController = MediaController(media3Controller, repoStore)

    override val navigationHost: MutableStateFlow<NavigationHost> = MutableStateFlow(
        NavigationHost(
            repoStore = repoStore,
            mediaController = mediaController,
            startDestination = NavigationHost.Destination.Library,
            toggleDrawerState = ::toggleDrawerState
        )
    )
    override val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val mainContent: MutableStateFlow<MainContent> = MutableStateFlow(MainContent.Content)
    override val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showAlbumDetails = {}, // TODO
        showArtistDetails = {} // TODO
    )
    override val player: Component = Player(
        mediaController = mediaController,
        showAlbumDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.AlbumDetails(it))
            mainContent.value = MainContent.Content
        },
        showArtistDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            mainContent.value = MainContent.Content
        },
        showQueue = { mainContent.value = MainContent.Queue },
        minimizePlayer = { mainContent.value = MainContent.Content }
    )
    override val queue: Component = Queue(
        mediaController = mediaController,
        close = { mainContent.value = MainContent.Player }
    )

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            modifier = modifier,
            darkTheme = darkTheme,
            mainContent = mainContent.asStateFlow(),
            navigationHost = navigationHost.asStateFlow(),
            player = player,
            miniPlayer = miniPlayer,
            queue = queue,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
            drawerState = drawerState.asStateFlow(),
            onExpandPlayerClick = { mainContent.value = MainContent.Player },
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
            mainContent: StateFlow<MainContent>,
            navigationHost: StateFlow<Component>,
            player: Component,
            miniPlayer: Component,
            queue: Component,
            onExpandPlayerClick: () -> Unit,
            selectedNavigationDrawerItem: StateFlow<NavigationDrawerItems>,
            drawerState: StateFlow<DrawerState>,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val mainContent by mainContent.collectAsState()
            val navigationHost by navigationHost.collectAsState()
            val drawerState by drawerState.collectAsState()
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
                                    when (mainContent) {
                                        MainContent.Content -> {
                                            navigationHost.show(Modifier.fillMaxWidth().weight(weight = 0.88f))
                                            miniPlayer.show(
                                                modifier = Modifier.clickable { onExpandPlayerClick() }
                                                    .fillMaxWidth()
                                                    .weight(0.12f)
                                            )
                                        }
                                        MainContent.Player -> { player.show(Modifier.fillMaxSize()) }
                                        MainContent.Queue -> { queue.show(Modifier.fillMaxSize()) }
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

    private enum class MainContent {
        Content, Player, Queue
    }
}