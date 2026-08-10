package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyArtist(
    val id: SpotifyArtistId,
    val spotifyId: String,
    val name: String,
    val smallImgId: MediaFileId?,
    val mediumImgId: MediaFileId?,
    val largeImgId: MediaFileId?,
    val apiResponse: String,
    val creationDatetime: Long
)