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

    private val settingsHost: Settings by lazy { Settings(repoStore) }
    private val musicLibraryHost: Component by lazy { MusicLibrary(repoStore.folderRepo, repoStore.playlistRepo, repoStore.trackRepo, repoStore.artistRepo, repoStore.albumRepo) }

    private val currentMainComponent: MutableStateFlow<Component> = MutableStateFlow(musicLibraryHost)
    private val selectedNavigationDrawerItem = MutableStateFlow(NavigationDrawerItems.MusicLibrary)

    @Composable
    override fun show(modifier: Modifier) {
        val currentMainComponent by currentMainComponent.collectAsState()
        val selectedNavigationDrawerItem by selectedNavigationDrawerItem.collectAsState()
        val darkTheme by darkTheme.collectAsState()

        Ui.Main(
            darkTheme = darkTheme,
            currentMainComponent = currentMainComponent,
            selectedNavigationDrawerItem = selectedNavigationDrawerItem,
            onNavigationDrawerItemClick = {
                when (it) {
                    NavigationDrawerItems.Settings -> mainComponentController.showSettings()
                    NavigationDrawerItems.MusicLibrary -> mainComponentController.showMusicLibrary()
                    NavigationDrawerItems.Playlists -> TODO()
                    NavigationDrawerItems.Albums -> TODO()
                    NavigationDrawerItems.Artists -> TODO()
                }
            }
        )
    }

    override fun clear() {
        settingsHost.clear()
        musicLibraryHost.clear()
        coroutineScope.cancel()
    }

    private inner class MainComponentController {
        fun showSettings() {
            currentMainComponent.update { settingsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Settings }
        }
        fun showMusicLibrary() {
            currentMainComponent.update { musicLibraryHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.MusicLibrary }
        }
    }

    private enum class NavigationDrawerItems(val label: String) {
        Settings("Settings"),
        MusicLibrary("Music Library"),
        Playlists("Playlists"),
        Albums("Albums"),
        Artists("Artists")
    }

    private object Ui {
        @Composable
        fun Main(
            darkTheme: DarkThemeOptions,
            currentMainComponent: Component,
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
                                modifier = Modifier.fillMaxWidth().weight(1f),
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // todo - you can show actions as icons here, otherwise delete this row
//                                            IconButton(
//                                                content = { Icon(Icons.Default.Refresh, null) },
//                                                onClick = onRefreshPlayer
//                                            )
                                        }
                                    }
                                },
                                content = { currentMainComponent.show(Modifier.fillMaxSize()) }
                            )
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