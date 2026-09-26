package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.playback.MediaPlayerState
import kotlinx.serialization.Serializable

@Serializable
sealed class MediaPlayerRpc : Rpc.ToClient() {
    @Serializable
    data object ObserveState : MediaPlayerRpc()

    @Serializable
    data class SetTrack(val id: MediaFileId) : MediaPlayerRpc()

    @Serializable
    data object Play : MediaPlayerRpc()

    @Serializable
    data object Pause : MediaPlayerRpc()

    @Serializable
    data object Stop : MediaPlayerRpc()

    @Serializable
    data class SetTime(val timeMillis: Long) : MediaPlayerRpc()

    @Serializable
    data class SetState(val value: MediaPlayerState) : MediaPlayerRpc()
}