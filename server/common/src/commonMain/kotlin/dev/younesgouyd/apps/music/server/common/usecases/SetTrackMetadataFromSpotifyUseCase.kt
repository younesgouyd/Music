package dev.younesgouyd.apps.music.server.common.usecases

import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.spotify.Track
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.repoes.SpotifyAlbumRepo
import dev.younesgouyd.apps.music.server.common.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.server.common.data.room.transactions.SetTrackMetadataFromSpotify
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SetTrackMetadataFromSpotifyUseCase(
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val trackRepo: TrackRepo,
    private val spotifyAlbumRepo: SpotifyAlbumRepo,
    private val transaction: SetTrackMetadataFromSpotify,
    private val spotify: Spotify,
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
            val spotifyTrackId = transaction.getSpotifyTrackId(spotifyApiTrack.id.value)?.let {
                SpotifyTrackId(it)
            }
            if (spotifyTrackId != null) {
                transaction.updateTrackSpotifyTrackId(spotifyTrackId, System.currentTimeMillis(), trackId)
            } else {
                transaction.addAlbumAndSet(
                    trackId = trackId,
                    spotifyApiTrack = spotifyApiTrack,
                    spotify = spotify,
                    fileManager = fileManager
                )
            }
        }
    }
}