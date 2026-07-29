package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrack(
    val id: SpotifyTrackId,
    val spotifyId: String, // TODO: (the same spotify track may have multiple ids because there's a copy for each market)
    val name: String,
    val spotifyAlbumId: SpotifyAlbumId,
    val discNumber: Int?,
    val trackNumber: Int?,
    val durationMs: Int?,
    val explicit: Boolean?,
    val apiResponse: String,
    val creationDatetime: Long
)
