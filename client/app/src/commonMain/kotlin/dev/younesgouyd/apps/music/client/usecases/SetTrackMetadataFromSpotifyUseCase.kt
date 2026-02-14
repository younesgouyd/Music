package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.SpotifyAlbumRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.data.room.transactions.SetTrackMetadataFromSpotify
import dev.younesgouyd.libs.music.spotifyapi.SpotifyApi
import dev.younesgouyd.libs.music.spotifyapi.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SetTrackMetadataFromSpotifyUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val trackRepo: TrackRepo,
    private val spotifyAlbumRepo: SpotifyAlbumRepo,
    private val transaction: SetTrackMetadataFromSpotify,
    private val spotifyApi: SpotifyApi,
    private val fileManager: FileManager
) {
    suspend fun execute(
        trackId: TrackId,
        spotifyApiTrack: Track
    ) {
        withContext(Dispatchers.IO) {
            val track = trackRepo.get(trackId).first()!! // TODO
            if (track.spotifyTrack != null) {
                val album = spotifyAlbumRepo.get(track.spotifyTrack.spotifyAlbumId).first()!! // TODO
                if (album.spotifyId != spotifyApiTrack.album.id.value) {
                    unsetSpotifyTrackUseCase.execute(trackId, track.spotifyTrack.id, track.spotifyTrack.spotifyAlbumId)
                }
            }
            val spotifyTrackId = transaction.getSpotifyTrackId(spotifyApiTrack.id.value)?.let { SpotifyTrackId(it) }
            if (spotifyTrackId != null) {
                transaction.updateTrackSpotifyTrackId(spotifyTrackId, System.currentTimeMillis(), trackId)
            } else {
                transaction.addAlbumAndSet(
                    trackId = trackId,
                    spotifyApiTrack = spotifyApiTrack,
                    spotifyApi = spotifyApi,
                    fileManager = fileManager
                )
            }
        }
    }
}