package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kmpalette.loader.ByteArrayLoader
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.rememberDynamicColorScheme
import com.materialkolor.palette.rememberThemeColor
import dev.younesgouyd.apps.music.client.multiplatform.ImageLoader
import dev.younesgouyd.apps.music.client.multiplatform.LocalImageLoader
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.MediaControllerState
import dev.younesgouyd.apps.music.client.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.client.multiplatform.usecases.*
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.YounesMusicTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Main(
    private val server: Server
) : Component() {
    override val title: String = ""

    private val repoStore: RepoStore = RepoStore(server)
    private val addToPlaylistUseCase = AddToPlaylistUseCase(server)
    private val deleteUseCase = DeleteUseCase(server)
    private val setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(server)
    private val tagItemsUseCase = TagItemsUseCase(server)
    private val unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(server)
    private val prepareImportUseCase = PrepareImportUseCase(server)
    private val moveItemsToFolderUseCase = MoveItemsToFolderUseCase(server)
    private val mediaController = MediaController(server)
    private val imageLoader = ImageLoader(server)

    private val darkTheme: StateFlow<DarkThemeOptions> = run {
        repoStore.settingRepo.getDarkTheme()
            .map { it?.value?.let { DarkThemeOptions.valueOf(it) } ?: DarkThemeOptions.SystemDefault }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), DarkThemeOptions.SystemDefault)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAlbumImage: StateFlow<ByteArray?> = mediaController.state.flatMapLatest {
        when (it) {
            is MediaControllerState.Unavailable -> flowOf(null)
            is MediaControllerState.Loading -> flowOf(null)
            is MediaControllerState.Available -> it.currentItem.map { it?.imageId }
        }
    }.mapLatest { imageId ->
        imageId?.let {
            server.getFile(imageId).takeIf { it.exists() }?.readBytes()
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val playerExpanded = MutableStateFlow(false)

    private val settings: Component = Settings(
        settingRepo = repoStore.settingRepo,
        spotifyAuthRepo = repoStore.spotifyAuthRepo
    )
    private var navigationHost: NavigationHost = getNewNavHost(NavigationHost.Destination.Home)
    private val miniPlayer: Component = MiniPlayer(
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
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationItems.Home)

    @Composable
    override fun show(modifier: Modifier) {
        val darkTheme by darkTheme.collectAsState()

        CompositionLocalProvider(LocalImageLoader provides imageLoader) {
            Ui.Main(
                modifier = modifier,
                darkTheme = darkTheme,
                mainComponent = mainComponent,
                player = player,
                miniPlayer = miniPlayer,
                playerExpanded = playerExpanded,
                queue = queue,
                selectedNavigationDrawerItem = selectedNavigationDrawerItem,
                currentAlbumImage = currentAlbumImage,
                onMinimizePlayerClick = ::minimizePlayer,
                onNavigationDrawerItemClick = ::handleNavigationDrawerItemClick
            )
        }
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

    private fun handleNavigationDrawerItemClick(item: NavigationItems) {
        when (item) {
            NavigationItems.Home -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.Home)
                mainComponent.value = navigationHost
            }
            NavigationItems.Library -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.Library)
                mainComponent.value = navigationHost
            }
            NavigationItems.Imports -> {
                navigationHost.clear()
                navigationHost = getNewNavHost(NavigationHost.Destination.ImportList)
                mainComponent.value = navigationHost
            }
            NavigationItems.Settings -> {
                mainComponent.value = settings
            }
        }
        selectedNavigationDrawerItem.value = item
        minimizePlayer()
    }

    private fun getNewNavHost(startDestination: NavigationHost.Destination): NavigationHost {
        return NavigationHost(
            repoStore = repoStore,
            addToPlaylistUseCase = addToPlaylistUseCase,
            deleteUseCase = deleteUseCase,
            setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
            tagItemsUseCase = tagItemsUseCase,
            unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
            prepareImportUseCase = prepareImportUseCase,
            moveItemsToFolderUseCase = moveItemsToFolderUseCase,
            mediaController = mediaController,
            startDestination = startDestination
        )
    }

    private enum class NavigationItems(val label: String, val icon: ImageVector) {
        Home("Home", Icons.Default.Home),
        Library("Library", Icons.Default.LibraryMusic),
        Imports("Imports", Icons.Default.Download),
        Settings("Settings", Icons.Default.Settings)
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
            selectedNavigationDrawerItem: StateFlow<NavigationItems>,
            currentAlbumImage: StateFlow<ByteArray?>,
            onMinimizePlayerClick: () -> Unit,
            onNavigationDrawerItemClick: (NavigationItems) -> Unit
        ) {
            val mainComponent by mainComponent.collectAsState()
            val playerExpanded by playerExpanded.collectAsState()
            val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()
            val currentAlbumImage by currentAlbumImage.collectAsState()

            YounesMusicTheme(
                colorScheme = createImageColorScheme(currentAlbumImage, darkTheme)
            ) {
                NavigationSuiteScaffold(
                    modifier = modifier,
                    navigationItemVerticalArrangement = Arrangement.Center,
                    navigationItems = {
                        NavigationItems.entries.forEach {
                            NavigationSuiteItem(
                                icon = { Icon(it.icon, null) },
                                label = { Text(it.label) },
                                selected = it == selectedNavigationDrawerItem,
                                onClick = { onNavigationDrawerItemClick(it) }
                            )
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

        @Composable
        private fun createImageColorScheme(
            imageBytes: ByteArray?,
            isDark: DarkThemeOptions
        ): ColorScheme {
            val systemDark = isSystemInDarkTheme()
            val darkMode = remember(isDark, systemDark) {
                when (isDark) {
                    DarkThemeOptions.SystemDefault -> systemDark
                    DarkThemeOptions.Disabled -> false
                    DarkThemeOptions.Enabled -> true
                }
            }

            val fallbackScheme = if (darkMode) darkColorScheme() else expressiveLightColorScheme()
            val bytes = imageBytes ?: return fallbackScheme

            val seedColor = rememberThemeColor(
                loader = ByteArrayLoader,
                input = bytes,
                fallback = fallbackScheme.primary
            )

            return rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = darkMode,
                style = PaletteStyle.Expressive
            )
        }
    }
}