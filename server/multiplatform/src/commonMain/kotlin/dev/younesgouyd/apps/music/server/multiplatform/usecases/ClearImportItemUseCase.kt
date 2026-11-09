package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.server.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.ClearImportSessionItem
import kotlinx.coroutines.flow.first

class ClearImportItemUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val trackRepo: TrackRepo,
    private val transaction: ClearImportSessionItem,
    private val fileManager: FileManager
) {
    suspend fun execute(id: ImportSessionItemId) {
        val track = trackRepo.getImportSessionTrack(id).first()!!
        if (track.spotifyTrackRelation != null) {
            unsetSpotifyTrackUseCase.execute(
                trackId = track.track.id,
                spotifyTrackId = track.spotifyTrackRelation.spotifyTrack.id,
                spotifyAlbumId = track.spotifyTrackRelation.spotifyTrack.spotifyAlbumId
            )
        }
        val file = transaction.execute(id)
        fileManager.delete(setOf(file))
    }
}