package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import dev.younesgouyd.apps.music.common.util.MediaPlayer
import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

abstract class Main(
    private val repoStore: RepoStore,
    private val mediaPlayer: MediaPlayer,
    private val mediaUtil: MediaUtil
) : Component() {
    override val title: String = ""
    protected val mainComponentController = MainComponentController()
    protected val darkTheme: StateFlow<DarkThemeOptions> = repoStore.settingsRepo.getDarkThemeFlow().filterNotNull().stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = DarkThemeOptions.SystemDefault
    )

    protected val mediaController = MediaController(
        trackRepo = repoStore.trackRepo,
        artistRepo = repoStore.artistRepo,
        albumRepo = repoStore.albumRepo,
        playlistRepo = repoStore.playlistRepo,
        playlistTrackCrossRefRepo = repoStore.playlistTrackCrossRefRepo,
        folderRepo = repoStore.folderRepo,
        onAlbumClick = mainComponentController::showAlbums,
        onArtistClick = mainComponentController::showArtists,
        mediaPlayer = mediaPlayer,
        mediaUtil = mediaUtil
    )

    protected val player = Player(mediaController)
    protected val queue = Queue(mediaController)

    protected abstract val settingsHost: Settings
    protected abstract val libraryHost: NavigationHost
    protected abstract val playlistsHost: NavigationHost
    protected abstract val artistsHost: NavigationHost
    protected abstract val albumsHost: NavigationHost

    protected abstract val currentMainComponent: MutableStateFlow<Component>
    protected abstract val selectedNavigationDrawerItem: MutableStateFlow<NavigationDrawerItems>

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        mediaController.release()
        settingsHost.clear()
        libraryHost.clear()
        coroutineScope.cancel()
    }

    protected inner class MainComponentController {
        fun showSettings() {
            currentMainComponent.update { settingsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Settings }
        }

        fun showLibrary() {
            currentMainComponent.update { libraryHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Library }
        }

        fun showPlaylists(id: Long?) {
            currentMainComponent.update { playlistsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Playlists }
            if (id != null) { playlistsHost.navigateTo(NavigationHost.Destination.PlaylistDetails(id)) }
        }

        fun showAlbums(id: Long?) {
            currentMainComponent.update { albumsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Albums }
            if (id != null) { albumsHost.navigateTo(NavigationHost.Destination.AlbumDetails(id)) }
        }

        fun showArtists(id: Long?) {
            currentMainComponent.update { artistsHost }
            selectedNavigationDrawerItem.update { NavigationDrawerItems.Artists }
            if (id != null) { artistsHost.navigateTo(NavigationHost.Destination.ArtistDetails(id)) }
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