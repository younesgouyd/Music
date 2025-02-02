package dev.younesgouyd.apps.music.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.app.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.app.data.repoes.PlaylistTrackCrossRefRepo
import dev.younesgouyd.apps.music.app.data.repoes.TrackRepo
import kotlinx.coroutines.cancel

class MoveToFolder(
    private val itemToMove: Item,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val dismiss: () -> Unit
) : Component() {
    override val title: String = "Move to Folder"

    @Composable
    override fun show(modifier: Modifier) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    sealed class Item {
        abstract val id: Long

        data class Track(override val id: Long) : Item()

        data class Playlist(override val id: Long) : Item()

        data class Folder(override val id: Long) : Item()
    }
}