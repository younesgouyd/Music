package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.UnsetSpotifyTrack

class UnsetSpotifyTrackUseCase(
    private val transaction: UnsetSpotifyTrack,
    private val fileManager: FileManager
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        val files = transaction.execute(trackId, spotifyTrackId, spotifyAlbumId)
        fileManager.delete(files)
    }
}