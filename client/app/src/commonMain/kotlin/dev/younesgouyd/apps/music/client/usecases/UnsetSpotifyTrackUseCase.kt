package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.UnsetSpotifyTrackDao

class UnsetSpotifyTrackUseCase(
    private val dao: UnsetSpotifyTrackDao,
    private val fileManager: FileManager
) {
    suspend fun execute(trackId: TrackId, spotifyTrackId: SpotifyTrackId, spotifyAlbumId: SpotifyAlbumId) {
        val files = dao.execute(trackId, spotifyTrackId, spotifyAlbumId)
        fileManager.delete(files)
    }
}