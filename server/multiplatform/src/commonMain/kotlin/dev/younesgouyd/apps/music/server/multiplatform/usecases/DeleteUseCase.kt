package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.TrackRelation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.ClearImportSessionItem
import kotlinx.coroutines.flow.first

class DeleteUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val fileManager: FileManager,
    private val clearImportItemTrx: ClearImportSessionItem
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>
    ) {
        for (trackId in tracks) {
            val track = trackRepo.get(trackId).first()
            if (track != null) {
                clearImportItem(track)
            }
        }
        for (playlist in playlists) { playlistRepo.delete(playlist) }
        for (folder in folders) { deleteFolderItems(folder) }
        for (folder in folders) { folderRepo.delete(folder) }
    }

    private suspend fun deleteFolderItems(id: FolderId) {
        val (tracks, playlists, folders) = getFolderItems(id)
        for (track in tracks) { clearImportItem(track) }
        for (playlist in playlists) { playlistRepo.delete(playlist) }
        for (folder in folders) { folderRepo.delete(folder) }
    }

    private suspend fun clearImportItem(track: TrackRelation) {
        if (track.spotifyTrackRelation != null) {
            unsetSpotifyTrackUseCase.execute(
                trackId = track.track.id,
                spotifyTrackId = track.spotifyTrackRelation.spotifyTrack.id,
                spotifyAlbumId = track.spotifyTrackRelation.spotifyTrack.spotifyAlbumId
            )
        }
        val file = clearImportItemTrx.execute(track.track.importSessionItemId)
        fileManager.delete(setOf(file))
    }

    private suspend fun getFolderItems(id: FolderId): Triple<Set<TrackRelation>, Set<PlaylistId>, Set<FolderId>> {
        val tracks = trackRepo.getFolderTracks(id).first().toMutableSet()
        val playlists = playlistRepo.getFolderPlaylists(id).first().map { it.id }.toMutableSet()
        val folders = folderRepo.getSubfolders(id).first().map { it.id }.toMutableSet()
        folders.toMutableSet().forEach {
            val (subImports, subPlaylists, subFolders) = getFolderItems(it)
            tracks.addAll(subImports)
            playlists.addAll(subPlaylists)
            folders.addAll(subFolders)
        }
        return Triple(tracks, playlists, folders)
    }
}