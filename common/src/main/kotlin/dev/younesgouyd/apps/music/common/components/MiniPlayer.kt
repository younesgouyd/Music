package dev.younesgouyd.apps.music.common.components

import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.QueueItem
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.RepeatState
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class MiniPlayer(
    mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Mini Player"
    protected val state: MutableStateFlow<MiniPlayerState> = MutableStateFlow(MiniPlayerState.Unavailable)

    init {
        coroutineScope.launch {
            mediaController.state.collectLatest { mediaControllerState ->
                state.value = when (mediaControllerState) {
                    is MediaController.MediaControllerState.Unavailable -> MiniPlayerState.Unavailable
                    is MediaController.MediaControllerState.Loading -> MiniPlayerState.Loading
                    is MediaController.MediaControllerState.Available -> MiniPlayerState.Available(
                        enabled = mediaControllerState.enabled,
                        timePositionChange = mediaControllerState.timePositionChange,
                        isPlaying = mediaControllerState.isPlaying,
                        repeatState = mediaControllerState.repeatState,
                        currentTrack = mediaControllerState.currentTrack,
                        onAlbumClick = showAlbumDetails,
                        onArtistClick = showArtistDetails,
                        onValueChange = mediaController::seek,
                        onPreviousClick = mediaController::previous,
                        onPlayClick = mediaController::play,
                        onPauseClick = mediaController::pause,
                        onNextClick = mediaController::next,
                        onRepeatClick = mediaController::repeat
                    )
                }
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    protected sealed class MiniPlayerState {
        data object Loading : MiniPlayerState()

        data object Unavailable : MiniPlayerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
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
            val onRepeatClick: () -> Unit
        ) : MiniPlayerState()
    }
}