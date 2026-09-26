package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.playback.QueueItemParameter
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed class MediaControllerRpc : Rpc.ToServer() {
    @Serializable
    data object ObserveState : MediaControllerRpc()

    @Serializable
    sealed class ObserveAvailableState : MediaControllerRpc() {
        @Serializable data object Clients : ObserveAvailableState()
        @Serializable data object SelectedClient : ObserveAvailableState()
        @Serializable data object Enabled : ObserveAvailableState()
        @Serializable data object Queue : ObserveAvailableState()
        @Serializable data object QueueItemIndex : ObserveAvailableState()
        @Serializable data object RepeatState : ObserveAvailableState()
        @Serializable data object MediaPlayerState : ObserveAvailableState()
    }

    @Serializable
    data class SelectClient(val id: Uuid) : MediaControllerRpc()

    @Serializable
    data class PlayQueue(
        val queue: List<QueueItemParameter>,
        val queueItemIndex: Int = 0
    ) : MediaControllerRpc()

    @Serializable
    data object Play : MediaControllerRpc()

    @Serializable
    data object Pause : MediaControllerRpc()

    @Serializable
    data class Seek(val positionMs: Long) : MediaControllerRpc()

    @Serializable
    data object Next : MediaControllerRpc()

    @Serializable
    data object Previous : MediaControllerRpc()

    @Serializable
    data object Repeat : MediaControllerRpc()

    @Serializable
    data class AddToQueue(val items: List<QueueItemParameter>) : MediaControllerRpc()

    @Serializable
    data class PlayItem(val index: Int) : MediaControllerRpc()

    @Serializable
    data class ChangeItemIndex(
        val oldIndex: Int,
        val newIndex: Int
    ) : MediaControllerRpc()

    @Serializable
    data class RemoveItem(val key: Int) : MediaControllerRpc()
}