package dev.younesgouyd.apps.music.client.app.multiplatform.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.app.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import dev.younesgouyd.apps.music.client.app.multiplatform.util.DarkThemeOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Main(
    private val repoStore: RepoStore,
    onReinitializeAppData: () -> Unit
) : Component() {
    override val title: String = ""
    private val darkTheme: StateFlow<DarkThemeOptions> = run {
        repoStore.settingsRepo.getDarkTheme()
            .map { DarkThemeOptions.valueOf(it.value) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), DarkThemeOptions.SystemDefault)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val playerExpanded = MutableStateFlow(false)

    private val settings: Component = Settings(
        settingsRepo = repoStore.settingsRepo,
        spotifyApi = repoStore.spotifyApi,
        onReinitializeAppData = onReinitializeAppData
    )
    private var navigationHost: NavigationHost = getNewNavHost(NavigationHost.Destination.Library)
    private val miniPlayer = MiniPlayer(
        mediaController = repoStore.mediaController,
        showAlbum = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.AlbumDetails(it))
        },
        showArtist = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
        },
        expand = ::expandPlayer
    )
    private val queue: Component = Queue(
        mediaController = repoStore.mediaController,
        showArtist = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            minimizePlayer()
        }
    )
    private val player: Component = Player(
        mediaController = repoStore.mediaController,
        showTack = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.TrackDetails(it))
            minimizePlayer()
        },
        showAlbum = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.AlbumDetails(it))
            minimizePlayer()
        },
        showArtist = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            minimizePlayer()
        },
        queue = queue::show
    )

    private val mainComponent: MutableStateFlow<Component> = MutableStateFlow(navigationHost)
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val drawerState: MutableStateFlow<DrawerState> = MutableStateFlow(DrawerState(initialValue = DrawerValue.Closed))

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            modifier = modifier,
            darkTheme = darkTheme,
            mainComponent = mainComponent.asStateFlow(),
            player = player,
            miniPlayer = miniPlayer,
            playerExpanded = playerExpanded,
            queue = queue,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
            drawerState = drawerState.asStateFlow(),
            onMinimizePlayerClick = ::minimizePlayer,
            onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
        )
    }

    override fun clear() {
        navigationHost.clear()
        coroutineScope.cancel()
    }

    private fun expandPlayer() {
        playerExpanded.value = true
    }

    private fun minimizePlayer() {
        playerExpanded.value = false
    }

    private suspend fun toggleDrawerState() {
        when (drawerState.value.currentValue) {
            DrawerValue.Open -> drawerState.value.close()
            DrawerValue.Closed -> drawerState.value.open()
        }
    }

    private fun handleNavigationDrawerItemClick(item: NavigationDrawerItems) {
        when (item) {
            NavigationDrawerItems.Settings -> {
                mainComponent.value = settings
            }
            NavigationDrawerItems.Tracks -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.TrackList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Library -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.Library)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Playlists -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.PlaylistList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Artists -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.ArtistList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Tags -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.TagList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Imports -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.ImportList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Export -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.Export)
                mainComponent.value = navigationHost
            }
        }
        selectedNavigationDrawerItem.value = item
        minimizePlayer()
    }

    private fun getNewNavHost(startDestination: NavigationHost.Destination): NavigationHost {
        return NavigationHost(
            toggleDrawerState = ::toggleDrawerState,
            repoStore = repoStore,
            startDestination = startDestination
        )
    }

    private enum class NavigationDrawerItems(val label: String) {
        Settings("Settings"),
        Tracks("Tracks"),
        Library("Library"),
        Playlists("Playlists"),
        Artists("Artists"),
        Tags("Tags"),
        Imports("Imports"),
        Export("Export")
    }

    private object Ui {
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

        @Composable
        fun Main(
            modifier: Modifier,
            darkTheme: DarkThemeOptions,
            mainComponent: StateFlow<Component>,
            player: Component,
            miniPlayer: Component,
            queue: Component,
            playerExpanded: StateFlow<Boolean>,
            selectedNavigationDrawerItem: StateFlow<NavigationDrawerItems>,
            drawerState: StateFlow<DrawerState>,
            onMinimizePlayerClick: () -> Unit,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val mainComponent by mainComponent.collectAsState()
            val drawerState by drawerState.collectAsState()
            val playerExpanded by playerExpanded.collectAsState()
            val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()

            YounesMusicTheme(darkTheme) {
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
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            when (playerExpanded) {
                                true -> {
                                    player.show(Modifier.fillMaxWidth().weight(1f, true))
                                    IconButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onMinimizePlayerClick,
                                        content = { Icon(Icons.Default.KeyboardArrowDown, null) }
                                    )
                                }
                                false -> {
                                    dev.younesgouyd.apps.music.client.app.multiplatform.components.util.AdaptiveUi(
                                        wide = {
                                            Row(Modifier.fillMaxWidth().weight(1f, true)) {
                                                mainComponent.show(Modifier.weight(.7f))
                                                queue.show(
                                                    Modifier.weight(.3f)
                                                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                                                )
                                            }
                                            miniPlayer.show(
                                                modifier = Modifier.fillMaxWidth()
                                                    .height(180.dp)
                                                    .padding(8.dp)
                                            )
                                        },
                                        compact = {
                                            mainComponent.show(Modifier.fillMaxWidth().weight(weight = 0.88f))
                                            miniPlayer.show(modifier = Modifier.fillMaxWidth().weight(0.12f))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}