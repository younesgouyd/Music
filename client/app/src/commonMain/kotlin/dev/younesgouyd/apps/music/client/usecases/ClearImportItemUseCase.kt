package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.data.room.entities.ClearImportSessionItemDao
import kotlinx.coroutines.flow.first

class ClearImportItemUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val trackRepo: TrackRepo,
    private val clearImportSessionItemDao: ClearImportSessionItemDao,
    private val fileManager: FileManager
) {
    suspend fun execute(id: ImportSessionItemId) {
        val track = trackRepo.getImportSessionTrack(id).first()!!
        if (track.spotifyTrack != null) {
            unsetSpotifyTrackUseCase.execute(
                trackId = track.track.id,
                spotifyTrackId = track.spotifyTrack.id,
                spotifyAlbumId = track.spotifyTrack.spotifyAlbumId
            )
        }
        val file = clearImportSessionItemDao.execute(id)
        fileManager.delete(setOf(file))
    }
}