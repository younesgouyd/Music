package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.ClearImportSessionItem
import kotlinx.coroutines.flow.first

class ClearImportItemUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val trackRepo: TrackRepo,
    private val transaction: ClearImportSessionItem,
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
        val file = transaction.execute(id)
        fileManager.delete(setOf(file))
    }
}