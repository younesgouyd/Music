package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TrackId

class MoveItemsToFolderUseCase(
    private val server: Server
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        folder: FolderId?
    ) {
        server.call(
            Rpc.ToServer.UseCase.MoveItemsToFolder(tracks, playlists, folders, folder)
        )
    }
}