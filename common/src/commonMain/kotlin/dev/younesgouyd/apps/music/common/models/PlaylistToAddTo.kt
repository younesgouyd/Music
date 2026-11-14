package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
sealed class PlaylistToAddTo {
    @Serializable data class Id(val value: PlaylistId) : PlaylistToAddTo()
    @Serializable data class New(val name: String) : PlaylistToAddTo()
}