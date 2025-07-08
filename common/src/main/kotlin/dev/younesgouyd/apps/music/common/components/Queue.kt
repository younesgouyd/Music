package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.QueueItem
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class Queue(
    protected val mediaController: MediaController
) : Component() {
    override val title: String = "Queue"
    protected val state: MutableStateFlow<QueueState> = MutableStateFlow(QueueState.Unavailable)

    init {
        coroutineScope.launch {
            mediaController.state.collectLatest { mediaControllerState ->
                state.value = when (mediaControllerState) {
                    is MediaController.MediaControllerState.Unavailable -> QueueState.Unavailable
                    is MediaController.MediaControllerState.Loading -> QueueState.Loading
                    is MediaController.MediaControllerState.Available -> QueueState.Available(
                        enabled = mediaControllerState.enabled,
                        queue = mediaControllerState.queue,
                        queueItemIndex = mediaControllerState.queueItemIndex,
                        queueSubItemIndex = mediaControllerState.queueSubItemIndex,
                        onPlayQueueItem = mediaController::playQueueItem,
                        onPlayQueueSubItem = mediaController::playTrackInQueue
                    )
                }
            }
        }
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    protected sealed class QueueState {
        data object Loading : QueueState()

        data object Unavailable : QueueState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val queue: List<QueueItem>,
            val queueItemIndex: Int,
            val queueSubItemIndex: Int,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) ->Unit
        ) : QueueState()
    }
}
