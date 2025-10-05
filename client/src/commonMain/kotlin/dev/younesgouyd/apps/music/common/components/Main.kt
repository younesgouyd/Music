package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.ImportService
import dev.younesgouyd.apps.music.common.components.util.AdaptiveUi
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.usecases.ImportFolderUseCase
import dev.younesgouyd.apps.music.common.usecases.SaveAudioFileAsTrackUseCase
import dev.younesgouyd.apps.music.common.usecases.SaveMp3FileAsTrackUseCase
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class Main(
    private val repoStore: RepoStore,
    mediaPlayer: MediaController.MediaPlayer,
    appDir: String
) : Component() {
    override val title: String = ""
    private val darkTheme: StateFlow<DarkThemeOptions> = repoStore.settingsRepo.getDarkTheme()
        .map {
            it!!.value?.let {
                DarkThemeOptions.valueOf(it)
            } ?: DarkThemeOptions.SystemDefault
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DarkThemeOptions.SystemDefault
        )

    private val mediaController = MediaController(mediaPlayer = mediaPlayer, repoStore = repoStore, appDir = appDir)
    private val importService = ImportService(
        server = repoStore.server,
        repo = repoStore.importSessionRepo,
        importFolderUseCase = ImportFolderUseCase(
            repoStore = repoStore,
            saveAudioFileAsTrackUseCase = SaveAudioFileAsTrackUseCase(repoStore),
            saveMp3FileAsTrackUseCase = SaveMp3FileAsTrackUseCase(repoStore)
        )
    )

    private val mainComponentType: MutableStateFlow<MainComponentType> = MutableStateFlow(MainComponentType.Content)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val playerExpanded: StateFlow<Boolean> = mainComponentType.mapLatest {
        it == MainComponentType.Player
    }.stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)

    private val imports: Component = Imports(repoStore.importSessionRepo)
    private val settings: Component = Settings(repoStore.settingsRepo)
    private var navigationHost: NavigationHost = getNewNavHost(NavigationHost.Destination.Library)
    private val miniPlayer = MiniPlayer(
        mediaController = mediaController,
        showAlbumDetails = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.AlbumDetails(it))
        },
        showArtistDetails = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.ArtistDetails(it))
        },
        expand = { mainComponentType.value = MainComponentType.Player }
    )
    private val player: Component = Player(
        mediaController = mediaController,
        showAlbumDetails = {
            mainComponent.value = navigationHost
            navigationHost.navigateTo(NavigationHost.Destination.AlbumDetails(it))
            mainComponentType.value = MainComponentType.Content
        },
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
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.Library)

    private val drawerState: MutableStateFlow<DrawerState> = MutableStateFlow(DrawerState(initialValue = DrawerValue.Closed))
    private val ongoingImportsCount: StateFlow<Long> = repoStore.importSessionRepo.getOngoingImportsCount()
        .stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = 0)

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
                    ongoingImportsCount = ongoingImportsCount,
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
                    ongoingImportsCount = ongoingImportsCount,
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
            NavigationDrawerItems.Albums -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.AlbumList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Artists -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.ArtistList)
                mainComponent.value = navigationHost
            }
            NavigationDrawerItems.Imports -> {
                mainComponent.value = imports
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
                ongoingImportsCount: StateFlow<Long>,
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
                val ongoingImportsCount by ongoingImportsCount.collectAsState()

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
                                                    onClick = { onNavigationDrawerItemClick(it) },
                                                    badge = if (it == NavigationDrawerItems.Imports && ongoingImportsCount > 0) {
                                                        { Badge { Text(ongoingImportsCount.toString()) } }
                                                    } else {
                                                        null
                                                    }
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
                                                mainComponent.show(Modifier.weight(.7f))
                                                queue.show(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp).weight(.3f))
                                            }
                                            miniPlayer.show(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(8.dp)
                                                    .weight(.2f)
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
                ongoingImportsCount: StateFlow<Long>,
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
                val ongoingImportsCount by ongoingImportsCount.collectAsState()

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
                                                    onClick = { onNavigationDrawerItemClick(it) },
                                                    badge = if (it == NavigationDrawerItems.Imports && ongoingImportsCount > 0) {
                                                        { Badge { Text(ongoingImportsCount.toString()) } }
                                                    } else {
                                                        null
                                                    }
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
                                            MainComponentType.Player -> { player.show(Modifier.fillMaxSize()) }
                                            MainComponentType.Queue -> { queue.show(Modifier.fillMaxSize()) }
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
        Artists("Artists"),
        Imports("Import")
    }

    private enum class MainComponentType {
        Content, Player, Queue
    }
}