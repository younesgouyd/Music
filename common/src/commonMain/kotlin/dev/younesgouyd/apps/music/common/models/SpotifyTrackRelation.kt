package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrackRelation(
    val spotifyTrack: SpotifyTrack,
    val track: Track?
)