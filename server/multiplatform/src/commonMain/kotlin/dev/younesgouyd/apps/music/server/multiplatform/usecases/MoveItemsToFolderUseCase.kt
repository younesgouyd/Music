package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.MoveItemsToFolder

class MoveItemsToFolderUseCase(
    private val transaction: MoveItemsToFolder
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        folder: FolderId?
    ) {
        transaction.execute(tracks, playlists, folders, folder)
    }
}