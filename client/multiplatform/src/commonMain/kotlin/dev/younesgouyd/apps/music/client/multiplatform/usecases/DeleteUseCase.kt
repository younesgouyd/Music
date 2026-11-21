package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TrackId

class DeleteUseCase(
    private val server: Server
) {
    suspend fun execute(
        tracks: Set<TrackId> = emptySet(),
        playlists: Set<PlaylistId> = emptySet(),
        folders: Set<FolderId> = emptySet(),
    ) {
        require(tracks.isNotEmpty() || playlists.isNotEmpty() || folders.isNotEmpty())
        server.call(
            Rpc.ToServer.UseCase.Delete(
                tracks = tracks,
                playlists = playlists,
                folders = folders
            )
        )
    }
}