package dev.younesgouyd.apps.music.common.components

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

abstract class Main(
    repoStore: RepoStore
) : Component() {
    override val title: String = ""
    protected val darkTheme: StateFlow<DarkThemeOptions> = repoStore.settingsRepo.getDarkThemeFlow().filterNotNull().stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = DarkThemeOptions.SystemDefault
    )

    protected abstract val mediaController: MediaController

    protected abstract val miniPlayer: Component
    protected abstract val player: Component
    protected abstract val queue: Component

    protected abstract val navigationHost: MutableStateFlow<NavigationHost>
    protected abstract val selectedNavigationDrawerItem: MutableStateFlow<NavigationDrawerItems>

    protected val drawerState: MutableStateFlow<DrawerState> = MutableStateFlow(DrawerState(initialValue = DrawerValue.Closed))

    override fun clear() {
        mediaController.release()
        navigationHost.value.clear()
        coroutineScope.cancel()
    }

    protected suspend fun toggleDrawerState() {
        when (drawerState.value.currentValue) {
            DrawerValue.Open -> drawerState.value.close()
            DrawerValue.Closed -> drawerState.value.open()
        }
    }

    protected enum class NavigationDrawerItems(val label: String) {
        Settings("Settings"),
        Library("Library"),
        Playlists("Playlists"),
        Albums("Albums"),
        Artists("Artists")
    }
}