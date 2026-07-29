package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class TrackRelation(
    val track: Track,
    val originalImport: ImportSessionItem,
    val spotifyTrack: SpotifyTrack?
)