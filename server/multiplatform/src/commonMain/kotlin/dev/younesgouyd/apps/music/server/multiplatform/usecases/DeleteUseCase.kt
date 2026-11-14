package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.TrackRepo
import kotlinx.coroutines.flow.first

class DeleteUseCase(
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val clearImportItemUseCase: ClearImportItemUseCase,
    private val folderRepo: FolderRepo
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>
    ) {
        for (trackId in tracks) {
            val track = trackRepo.get(trackId).first()
            if (track != null) {
                clearImportItemUseCase.execute(track.track.importSessionItemId)
            }
        }
        for (playlist in playlists) { playlistRepo.delete(playlist) }
        for (folder in folders) { deleteFolderItems(folder) }
        for (folder in folders) { folderRepo.delete(folder) }
    }

    private suspend fun deleteFolderItems(id: FolderId) {
        val (imports, playlists, folders) = getFolderItems(id)
        for (import in imports) { clearImportItemUseCase.execute(import) }
        for (playlist in playlists) { playlistRepo.delete(playlist) }
        for (folder in folders) { folderRepo.delete(folder) }
    }

    private suspend fun getFolderItems(id: FolderId): Triple<Set<ImportSessionItemId>, Set<PlaylistId>, Set<FolderId>> {
        val imports = trackRepo.getFolderTracks(id).first().map { it.track.importSessionItemId }.toMutableSet()
        val playlists = playlistRepo.getFolderPlaylists(id).first().map { it.id }.toMutableSet()
        val folders = folderRepo.getSubfolders(id).first().map { it.id }.toMutableSet()
        folders.toMutableSet().forEach {
            val (subImports, subPlaylists, subFolders) = getFolderItems(it)
            imports.addAll(subImports)
            playlists.addAll(subPlaylists)
            folders.addAll(subFolders)
        }
        return Triple(imports, playlists, folders)
    }
}