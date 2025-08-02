package dev.younesgouyd.apps.music.common

import com.mpatric.mp3agic.Mp3File
import dev.younesgouyd.apps.music.common.data.RepoStore
import java.io.File

class SaveMp3FileAsTrackUseCase(
    private val repoStore: RepoStore
) {
    private val trackRepo get() = repoStore.trackRepo
    private val artistRepo get() = repoStore.artistRepo
    private val albumRepo get() = repoStore.albumRepo
    private val artistTrackCrossRefRepo get() = repoStore.artistTrackCrossRefRepo

    suspend fun execute(file: File, uri: String, parentFolderId: Long) {
        val mp3file = Mp3File(file)
        var title: String? = null
        var albumTrackNumber: Long? = null
        var artist: String? = null
        var album: String? = null
        var lyrics: String? = null
        var year: String? = null
        var albumImage: ByteArray? = null
        if (mp3file.hasId3v2Tag()) {
            val id3 = mp3file.id3v2Tag
            val albumImageData = id3.albumImage
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
            lyrics = id3.lyrics
            albumImage = albumImageData
        } else if (mp3file.hasId3v1Tag()) {
            val id3 = mp3file.id3v1Tag
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
        }
        var artistId: Long? = null
        var albumId: Long? = null
        if (!artist.isNullOrEmpty()) {
            val artists = artistRepo.getByName(artist)
            if (artists.isEmpty()) {
                artistId = artistRepo.add(name = artist, image = null)
            } else if (artists.size == 1) {
                artistId = artists.first().id
            }
        }
        if (!album.isNullOrEmpty()) {
            val albums = albumRepo.getByName(album)
            if (albums.isEmpty()) {
                albumId = albumRepo.add(name = album, image = albumImage, releaseDate = year)
            } else if (albums.size == 1) {
                albumId = albums.first().id
            }
        }
        val trackId = trackRepo.add(
            name = if (!title.isNullOrEmpty()) title else file.name,
            folderId = parentFolderId,
            albumId = albumId,
            audioUri = uri,
            videoUrl = null,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            duration = mp3file.lengthInMilliseconds
        )
        if (artistId != null) {
            artistTrackCrossRefRepo.add(artistId, trackId)
        }
    }
}