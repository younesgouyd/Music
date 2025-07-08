package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.QueueItem
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.RepeatState
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
abstract class Player(
    mediaController: MediaController
) : Component() {
    override val title: String = ""
    protected val state: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.Unavailable)

    init {
        coroutineScope.launch {
            mediaController.state.collectLatest { mediaControllerState ->
                state.value = when (mediaControllerState) {
                    is MediaController.MediaControllerState.Unavailable -> PlayerState.Unavailable
                    is MediaController.MediaControllerState.Loading -> PlayerState.Loading
                    is MediaController.MediaControllerState.Available -> PlayerState.Available(
                        enabled = mediaControllerState.enabled,
                        queue = mediaControllerState.queue,
                        queueItemIndex = mediaControllerState.queueItemIndex,
                        queueSubItemIndex = mediaControllerState.queueSubItemIndex,
                        timePositionChange = mediaControllerState.timePositionChange,
                        isPlaying = mediaControllerState.isPlaying,
                        repeatState = mediaControllerState.repeatState,
                        currentTrack = mediaControllerState.currentTrack,
                        onAlbumClick = { TODO() },
                        onArtistClick = { TODO() },
                        onValueChange = mediaController::seek,
                        onPreviousClick = mediaController::previous,
                        onPlayClick = mediaController::play,
                        onPauseClick = mediaController::pause,
                        onNextClick = mediaController::next,
                        onRepeatClick = mediaController::repeat,
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

    sealed class PlayerState {
        data object Loading : PlayerState()

        data object Unavailable : PlayerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val queue: List<QueueItem>,
            val queueItemIndex: Int,
            val queueSubItemIndex: Int,
            val timePositionChange: StateFlow<Long>,
            val isPlaying: StateFlow<Boolean>,
            val repeatState: RepeatState,
            val currentTrack: QueueItem.Track,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onValueChange: (Long) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayClick: () -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onRepeatClick: () -> Unit,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) ->Unit,
        ) : PlayerState()
    }
}