package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.transactions.UnsetSpotifyTrack

class UnsetSpotifyTrackUseCase(
    private val transaction: UnsetSpotifyTrack,
    private val fileManager: FileManager
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        val files = transaction.execute(trackId, spotifyTrackId, spotifyAlbumId)
        fileManager.delete(files)
    }
}