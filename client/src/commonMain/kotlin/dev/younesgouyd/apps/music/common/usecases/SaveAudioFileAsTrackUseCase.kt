package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Base64String
import dev.younesgouyd.apps.music.common.data.RepoStore
import kotlinx.coroutines.flow.first
import kotlin.time.Duration

class SaveAudioFileAsTrackUseCase(
    private val repoStore: RepoStore
) {
    private val trackRepo get() = repoStore.trackRepo
    private val artistRepo get() = repoStore.artistRepo
    private val artistTrackCrossRefRepo get() = repoStore.artistTrackCrossRefRepo

    suspend fun execute(
        folderId: Long?,
        title: String,
        duration: Duration,
        artists: List<String>,
        album: String?,
        albumTrackNumber: Int?,
        lyrics: String?,
        albumImage: Base64String?
    ): Long {
        require(title.isNotBlank())
        val artistIds = saveArtists(artists)
        val trackId = trackRepo.add(
            name = title,
            folderId = folderId,
            album = album,
            albumArt = albumImage,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            duration = duration
        )
        for (artistId in artistIds) {
            artistTrackCrossRefRepo.add(artistId, trackId)
        }
        return trackId
    }

    private suspend fun saveArtists(artists: List<String>): List<Long> {
        return buildList {
            for (artist in artists) {
                val dbArtists = artistRepo.getByName(artist).first()
                val artistId = if (dbArtists.isEmpty()) {
                    artistRepo.add(name = artist, image = null)
                } else {
                    if (dbArtists.size > 1) {
                        TODO("found multiple artists with same name")
                    } else {
                        dbArtists.first().id
                    }
                }
                this.add(artistId)
            }
        }
    }
}