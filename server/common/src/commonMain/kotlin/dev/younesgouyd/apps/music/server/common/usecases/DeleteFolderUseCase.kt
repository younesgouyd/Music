package dev.younesgouyd.apps.music.server.common.usecases

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.server.common.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.server.common.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.server.common.data.repoes.TrackRepo
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