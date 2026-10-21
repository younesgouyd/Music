package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.RepeatState
import dev.younesgouyd.apps.music.common.models.datamodels.Client
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem
import kotlinx.coroutines.flow.StateFlow

sealed class MediaControllerState {
    data object Loading : MediaControllerState()

    data object Unavailable : MediaControllerState()

    data class Available(
        val clients: StateFlow<List<Client>>,
        val selectedClient: StateFlow<ClientId>,
        val enabled: StateFlow<Boolean>,
        val queue: StateFlow<List<QueueItem>>,
        val queueItemIndex: StateFlow<Int>,
        val currentItem: StateFlow<QueueItem?>,
        val repeatState: StateFlow<RepeatState>,
        val positionMs: StateFlow<Long?>,
        val mediaPlayerState: StateFlow<MediaPlayerState?>
    ) : MediaControllerState()
}