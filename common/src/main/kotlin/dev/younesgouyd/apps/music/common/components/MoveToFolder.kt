package dev.younesgouyd.apps.music.common.components

import dev.younesgouyd.apps.music.common.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.common.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.common.data.repoes.PlaylistTrackCrossRefRepo
import dev.younesgouyd.apps.music.common.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel

abstract class MoveToFolder(
    private val itemToMove: Item,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val dismiss: () -> Unit
) : Component() {
    override val title: String = "Move to Folder"

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