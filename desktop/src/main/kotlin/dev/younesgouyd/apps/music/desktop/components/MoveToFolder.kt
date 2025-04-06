package dev.younesgouyd.apps.music.desktop.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.MoveToFolder
import dev.younesgouyd.apps.music.common.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.common.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.common.data.repoes.PlaylistTrackCrossRefRepo
import dev.younesgouyd.apps.music.common.data.repoes.TrackRepo

class MoveToFolder(
    itemToMove: Item,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    playlistRepo: PlaylistRepo,
    dismiss: () -> Unit
) : MoveToFolder(itemToMove, playlistTrackCrossRefRepo, trackRepo, folderRepo, playlistRepo, dismiss) {
    @Composable
    override fun show(modifier: Modifier) {
        TODO("Not yet implemented")
    }
}