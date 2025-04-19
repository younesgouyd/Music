package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

abstract class NavigationHost(
    protected val navController: NavigationController,
    protected val toggleDrawerState: suspend () -> Unit
) : Component() {
    override val title: String = ""

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        navController.dispose()
        coroutineScope.cancel()
    }

    fun navigateTo(destination: Destination) {
        navController.navigateTo(destination)
    }

    sealed class Destination {
        data object Library : Destination()

        data object PlaylistList : Destination()

        data class PlaylistDetails(val playlistId: Long): Destination()

        data object ArtistList : Destination()

        data class ArtistDetails(val artistId: Long) : Destination()

        data object AlbumList : Destination()

        data class AlbumDetails(val albumId: Long) : Destination()
    }

    abstract class NavigationController {
        abstract val inHome: StateFlow<Boolean>
        abstract val currentDestination: StateFlow<Component>

        abstract fun navigateTo(destination: Destination)

        abstract fun navigateBack()

        abstract fun dispose()
    }
}
