package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import dev.younesgouyd.apps.music.client.app.multiplatform.data.FolderId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.TrackRepo
import kotlinx.coroutines.flow.first

class DeleteFolderUseCase(
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val clearImportItemUseCase: ClearImportItemUseCase,
    private val folderRepo: FolderRepo
) {
    suspend fun execute(id: FolderId) {
        val playlists = playlistRepo.getFolderPlaylists(id).first()
        for (playlist in playlists) {
            playlistRepo.delete(playlist.id)
        }
        val tracks = trackRepo.getFolderTracks(id).first()
        for (track in tracks) {
            clearImportItemUseCase.execute(track.importSessionItemId)
        }
        folderRepo.delete(id)
    }
}