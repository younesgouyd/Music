package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.client.multiplatform.usecases.*
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.YounesMusicTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Main(
    server: Server
) : Component() {
    override val title: String = ""

    private val repoStore: RepoStore = RepoStore(server)
    private val deleteFolderUseCase = DeleteFolderUseCase(server)
    private val clearImportItemUseCase = ClearImportItemUseCase(server)
    private val setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(server)
    private val unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(server)
    private val prepareImportUseCase = PrepareImportUseCase(server)
    private val mediaController = MediaController(server)

    private val darkTheme: StateFlow<DarkThemeOptions> = run {
        repoStore.settingRepo.getDarkTheme()
            .map { it?.value?.let { DarkThemeOptions.valueOf(it) } ?: DarkThemeOptions.SystemDefault }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), DarkThemeOptions.SystemDefault)
    }

    private val playerExpanded = MutableStateFlow(false)

    private val settings: Component = Settings(
        settingRepo = repoStore.settingRepo,
        spotifyAuthRepo = repoStore.spotifyAuthRepo
    )
    private var navigationHost: NavigationHost = getNewNavHost(NavigationHost.Destination.Library)
    private val miniPlayer = MiniPlayer(
        mediaController = mediaController,
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
        mediaController = mediaController,
        showArtist = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            minimizePlayer()
        }
    )
    private val player: Component = Player(
        mediaController = mediaController,
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

    private val isDrawerOpen = MutableStateFlow(false)

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
            isDrawerOpen = isDrawerOpen.asStateFlow(),
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
        }
        selectedNavigationDrawerItem.value = item
        minimizePlayer()
    }

    private fun getNewNavHost(startDestination: NavigationHost.Destination): NavigationHost {
        return NavigationHost(
            toggleDrawerState = { isDrawerOpen.update { !it } },
            repoStore = repoStore,
            deleteFolderUseCase = deleteFolderUseCase,
            clearImportItemUseCase = clearImportItemUseCase,
            setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
            unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
            prepareImportUseCase = prepareImportUseCase,
            mediaController = mediaController,
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
        Imports("Imports")
    }

    private object Ui {
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
            isDrawerOpen: StateFlow<Boolean>,
            onMinimizePlayerClick: () -> Unit,
            onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
        ) {
            val mainComponent by mainComponent.collectAsState()
            val isDrawerOpen by isDrawerOpen.collectAsState()
            val drawerState = rememberDrawerState(initialValue = if (isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)
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
                                    dev.younesgouyd.apps.music.client.multiplatform.components.util.AdaptiveUi(
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

            LaunchedEffect(isDrawerOpen) {
                if (isDrawerOpen) drawerState.open() else drawerState.close()
            }
        }
    }
}