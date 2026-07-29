package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAlbum(
    val id: SpotifyAlbumId,
    val spotifyId: String,
    val name: String,
    val albumType: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val smallImgId: MediaFileId?,
    val mediumImgId: MediaFileId?,
    val largeImgId: MediaFileId?,
    val apiResponse: String,
    val creationDatetime: Long
)