package dev.younesgouyd.apps.music.server.common.usecases

import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.room.transactions.UnsetSpotifyTrack

class UnsetSpotifyTrackUseCase(
    private val transaction: UnsetSpotifyTrack,
    private val fileManager: FileManager
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        val files = transaction.execute(trackId, spotifyTrackId, spotifyAlbumId)
        fileManager.delete(files)
    }
}