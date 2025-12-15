package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.ImportService
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.usecases.ImportFromInternetUseCase
import dev.younesgouyd.apps.music.client.usecases.ImportLocalFileUseCaseImpl
import dev.younesgouyd.apps.music.client.usecases.SaveAudioFileAsTrackUseCase
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class Main(
    private val repoStore: RepoStore,
    mediaPlayer: MediaController.MediaPlayer,
    onReinitializeAppData: () -> Unit
) : Component() {
    override val title: String = ""
    private val darkTheme: StateFlow<DarkThemeOptions> =
        repoStore.settingsRepo.getDarkTheme()
            .map {
                it!!.value?.let {
                    DarkThemeOptions.valueOf(it)
                } ?: DarkThemeOptions.SystemDefault
            }.stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DarkThemeOptions.SystemDefault
            )

    private val mediaController = MediaController(
        mediaPlayer = mediaPlayer,
        repoStore = repoStore
    )
    private val importService: ImportService = run {
        val saveAudioFileAsTrackUseCase = SaveAudioFileAsTrackUseCase(repoStore)
        ImportService(
            importSessionRepo = repoStore.importSessionRepo,
            importSessionItemRepo = repoStore.importSessionItemRepo,
            playlistRepo = repoStore.playlistRepo,
            playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
            mediaFileRepo = repoStore.mediaFileRepo,
            mediaFilePlaylistCrossRefRepo = repoStore.mediaFilePlaylistCrossRefRepo,
            importLocalFileUseCase = ImportLocalFileUseCaseImpl(
                mediaFileRepo = repoStore.mediaFileRepo,
                mediaFileTrackCrossRefRepo = repoStore.mediaFileTrackCrossRefRepo,
                saveAudioFileAsTrackUseCase = saveAudioFileAsTrackUseCase
            ),
            importFromInternetUseCase = ImportFromInternetUseCase(
                mediaFileRepo = repoStore.mediaFileRepo,
                mediaFileTrackCrossRefRepo = repoStore.mediaFileTrackCrossRefRepo,
                server = repoStore.server,
                saveAudioFileAsTrackUseCase = saveAudioFileAsTrackUseCase
            )
        )
    }

    private val mainComponentType: MutableStateFlow<MainComponentType> = MutableStateFlow(MainComponentType.Content)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val playerExpanded: StateFlow<Boolean> = mainComponentType.mapLatest {
        it == MainComponentType.Player
    }.stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)

    private val settings: Component = Settings(
        repoStore.settingsRepo,
        onReinitializeAppData = onReinitializeAppData
    )
    private var navigationHost: NavigationHost = getNewNavHost(NavigationHost.Destination.TrackList)
    private val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showArtistDetails = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
        },
        expand = { mainComponentType.value = MainComponentType.Player }
    )
    private val player: Component = Player(
        mediaController = mediaController,
        showArtistDetails = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
            mainComponentType.value = MainComponentType.Content
        },
        showQueue = { mainComponentType.value = MainComponentType.Queue },
        minimizePlayer = { mainComponentType.value = MainComponentType.Content }
    )
    private val queue: Component = Queue(
        mediaController = mediaController,
        close = { mainComponentType.value = MainComponentType.Player }
    )

    private val mainComponent: MutableStateFlow<Component> = MutableStateFlow(navigationHost)
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Tracks)

    private val drawerState: MutableStateFlow<DrawerState> =
        MutableStateFlow(DrawerState(initialValue = DrawerValue.Closed))

    init {
        importService.start()
    }

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        AdaptiveUi(
            wide = {
                Ui.Wide.Main(
                    modifier = modifier,
                    darkTheme = darkTheme,
                    mainComponent = mainComponent.asStateFlow(),
                    player = player,
                    miniPlayer = miniPlayer,
                    playerExpanded = playerExpanded,
                    queue = queue,
                    selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
                    drawerState = drawerState.asStateFlow(),
                    onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
                )
            },
            compact = {
                Ui.Compact.Main(
                    modifier = modifier,
                    darkTheme = darkTheme,
                    mainComponentType = mainComponentType.asStateFlow(),
                    mainComponent = mainComponent.asStateFlow(),
                    player = player,
                    miniPlayer = miniPlayer,
                    queue = queue,
                    selectedNavigationDrawerItem = selectedNavigationDrawerItem.asStateFlow(),
                    drawerState = drawerState.asStateFlow(),
                    onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
                )
            }
        )
    }

    override fun clear() {
        mediaController.release()
        runBlocking {
            importService.stop()
        }
        navigationHost.clear()
        coroutineScope.cancel()
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
        mainComponentType.value = MainComponentType.Content
        selectedNavigationDrawerItem.value = item
    }

    private fun getNewNavHost(startDestination: NavigationHost.Destination): NavigationHost {
        return NavigationHost(
            toggleDrawerState = ::toggleDrawerState,
            repoStore = repoStore,
            mediaController = mediaController,
            startDestination = startDestination,
        )
    }

    private object Ui {
        object Wide {
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
                onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
            ) {
                val mainComponent by mainComponent.collectAsState()
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
                                                modifier = Modifier.fillMaxWidth()
                                                    .weight(weight = 1f)
                                            ) {
                                                mainComponent.show(Modifier.weight(.7f))
                                                queue.show(
                                                    Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp).weight(.3f)
                                                )
                                            }
                                            miniPlayer.show(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(8.dp)
                                                    .height(180.dp)
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
                mainComponentType: StateFlow<MainComponentType>,
                mainComponent: StateFlow<Component>,
                player: Component,
                miniPlayer: Component,
                queue: Component,
                selectedNavigationDrawerItem: StateFlow<NavigationDrawerItems>,
                drawerState: StateFlow<DrawerState>,
                onNavigationDrawerItemClick: (NavigationDrawerItems) -> Unit
            ) {
                val mainComponentType by mainComponentType.collectAsState()
                val mainComponent by mainComponent.collectAsState()
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
                                        when (mainComponentType) {
                                            MainComponentType.Content -> {
                                                mainComponent.show(Modifier.fillMaxWidth().weight(weight = 0.88f))
                                                miniPlayer.show(modifier = Modifier.fillMaxWidth().weight(0.12f))
                                            }
                                            MainComponentType.Player -> {
                                                player.show(Modifier.fillMaxSize())
                                            }
                                            MainComponentType.Queue -> {
                                                queue.show(Modifier.fillMaxSize())
                                            }
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
        Tracks("Tracks"),
        Library("Library"),
        Playlists("Playlists"),
        Artists("Artists"),
        Tags("Tags"),
        Imports("Imports"),
        Export("Export")
    }

    private enum class MainComponentType {
        Content, Player, Queue
    }
}