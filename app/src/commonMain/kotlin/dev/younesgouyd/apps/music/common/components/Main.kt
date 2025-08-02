package dev.younesgouyd.apps.music.common.components

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
import dev.younesgouyd.apps.music.common.components.util.AdaptiveUi
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Main(
    private val repoStore: RepoStore,
    mediaPlayer: MediaController.MediaPlayer,
    private val isPlaying: MutableStateFlow<Boolean>,
    private val timePositionChange: MutableStateFlow<Long>
) : Component() {
    override val title: String = ""
    private val darkTheme: StateFlow<DarkThemeOptions> = repoStore.settingsRepo.getDarkThemeFlow().filterNotNull().stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = DarkThemeOptions.SystemDefault
    )

    private val mediaController: MediaController = run {
        MediaController(
            mediaPlayer = mediaPlayer,
            repoStore = repoStore,
            isPlaying = isPlaying,
            timePositionChange = timePositionChange
        )
    }

    private val mainContent: MutableStateFlow<MainContent> = MutableStateFlow(MainContent.Content)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val playerExpanded: StateFlow<Boolean> = mainContent.mapLatest {
        it == MainContent.Player
    }.stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)
    private val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showAlbumDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.AlbumDetails(it))
        },
        showArtistDetails = {
            navigationHost.value.navigateTo(NavigationHost.Destination.ArtistDetails(it))
        }
    )
    private val player: Component = Player(
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
    private val queue: Component = Queue(
        mediaController = mediaController,
        close = { mainContent.value = MainContent.Player }
    )


    private val navigationHost: MutableStateFlow<NavigationHost> = MutableStateFlow(
        NavigationHost(
            repoStore = repoStore,
            mediaController = mediaController,
            startDestination = NavigationHost.Destination.Library,
            toggleDrawerState = ::toggleDrawerState
        )
    )
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val drawerState: MutableStateFlow<DrawerState> = MutableStateFlow(DrawerState(initialValue = DrawerValue.Closed))

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        AdaptiveUi(
            wide = {
                Ui.Wide.Main(
                    modifier = modifier,
                    darkTheme = darkTheme,
                    navigationHost = navigationHost.asStateFlow(),
                    player = player,
                    miniPlayer = miniPlayer,
                    playerExpanded = playerExpanded,
                    queue = queue,
                    selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
                    drawerState = drawerState.asStateFlow(),
                    onExpandPlayerClick = { mainContent.value = MainContent.Player },
                    onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
                )
            },
            compact = {
                Ui.Compact.Main(
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
        )
    }

    override fun clear() {
        mediaController.release()
        navigationHost.value.clear()
        coroutineScope.cancel()
    }

    private suspend fun toggleDrawerState() {
        when (drawerState.value.currentValue) {
            DrawerValue.Open -> drawerState.value.close()
            DrawerValue.Closed -> drawerState.value.open()
        }
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
        object Wide {
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

        object Compact {
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
    }

    private enum class NavigationDrawerItems(val label: String) {
        Settings("Settings"),
        Library("Library"),
        Playlists("Playlists"),
        Albums("Albums"),
        Artists("Artists")
    }

    private enum class MainContent {
        Content, Player, Queue
    }
}