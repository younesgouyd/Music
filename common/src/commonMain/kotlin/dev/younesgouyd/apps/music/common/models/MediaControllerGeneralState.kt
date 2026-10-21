package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
enum class MediaControllerGeneralState {
    Unavailable, Loading, Available
}