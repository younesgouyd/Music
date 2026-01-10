package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.MediaController.MediaControllerState.Available.RepeatState
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class Player(
    mediaController: MediaController,
    showTack: (TrackId) -> Unit,
    showArtistDetails: (ArtistId) -> Unit,
    queue: @Composable (Modifier) -> Unit
) : Component() {
    override val title: String = "Player"
    private var windowSizeClass: WindowSizeClass? = null
    private val state: StateFlow<PlayerState>

    init {
        val isTrackVisible = MutableStateFlow(true)
        val isQueueVisible = MutableStateFlow(false)

        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> PlayerState.Unavailable
                is MediaController.MediaControllerState.Loading -> PlayerState.Loading
                is MediaController.MediaControllerState.Available -> PlayerState.Available(
                    enabled = mediaControllerState.enabled,
                    timePositionChange = mediaControllerState.timePositionChange,
                    isPlaying = mediaControllerState.isPlaying,
                    track = mediaControllerState.currentItem,
                    isTrackVisible = isTrackVisible.asStateFlow(),
                    isQueueVisible = isQueueVisible.asStateFlow(),
                    repeatState = mediaControllerState.repeatState,
                    queue = queue,
                    onToggleTrackVisibility = {
                        when (windowSizeClass) {
                            WindowSizeClass.Wide -> isTrackVisible.update { !it }
                            WindowSizeClass.Compact -> {
                                isTrackVisible.update {
                                    if (!it) { isQueueVisible.value = false }
                                    !it
                                }
                            }
                            null -> TODO()
                        }
                    },
                    onToggleQueueVisibility = {
                        when (windowSizeClass) {
                            WindowSizeClass.Wide -> isQueueVisible.update { !it }
                            WindowSizeClass.Compact -> {
                                isQueueVisible.update {
                                    if (!it) { isTrackVisible.value = false }
                                    !it
                                }
                            }
                            null -> TODO()
                        }
                    },
                    onTrackNameClick = showTack,
                    onArtistClick = showArtistDetails,
                    onTimeChange = mediaController::seek,
                    onPreviousClick = mediaController::previous,
                    onPlayClick = mediaController::play,
                    onPauseClick = mediaController::pause,
                    onNextClick = mediaController::next,
                    onRepeatClick = mediaController::repeat
                )
            }
        }.stateIn(coroutineScope, SharingStarted.Lazily, PlayerState.Unavailable)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        AdaptiveUi(
            wide = { Ui.Wide.Main(modifier = modifier, state = state) },
            compact = { Ui.Compact.Main(modifier = modifier, state = state) },
            onStateChange = { windowSizeClass = it }
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class PlayerState {
        data object Loading : PlayerState()

        data object Unavailable : PlayerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val timePositionChange: StateFlow<Duration>,
            val isPlaying: StateFlow<Boolean>,
            val track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
            val isTrackVisible: StateFlow<Boolean>,
            val isQueueVisible: StateFlow<Boolean>,
            val repeatState: StateFlow<RepeatState>,
            val queue: @Composable (Modifier) -> Unit,
            val onToggleQueueVisibility: () -> Unit,
            val onToggleTrackVisibility: () -> Unit,
            val onTrackNameClick: (TrackId) -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onTimeChange: (Duration) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayClick: () -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onRepeatClick: () -> Unit
        ) : PlayerState()
    }

    private object Ui {
        private object Common {
            @Composable
            fun TrackInfo(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem,
                onTrackNameClick: () -> Unit,
                onArtistClick: (ArtistId) -> Unit
            ) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onTrackNameClick
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.displayMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (track.album != null) {
                        Surface {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Album, null)
                                Text(
                                    text = track.album,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(track.artists) { artist ->
                            TextButton(
                                onClick = { onArtistClick(artist.id) }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null)
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            fun PlaybackControls(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: MediaController.MediaControllerState.Available.QueueItem,
                isPlaying: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                timePositionChange: StateFlow<Duration>,
                onTimeChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val repeatState by repeatState.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val formattedDuration = remember(track.duration) { track.duration.formatted() }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlaybackSlider(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = enabled,
                        duration = track.duration,
                        currentPosition = timePositionChange,
                        onSeek = onTimeChange
                    )
                    Text(
                        text = "${timePositionChange.formatted()}/${formattedDuration}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(32.dp))
                        FilledTonalIconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onPreviousClick,
                            content = { Icon(Icons.Default.SkipPrevious, null) }
                        )
                        FilledIconToggleButton(
                            modifier = Modifier.size(56.dp),
                            checked = isPlaying,
                            enabled = enabled,
                            onCheckedChange = { if (isPlaying) onPauseClick() else onPlayClick() }
                        ) {
                            if (isPlaying) {
                                Icon(Icons.Default.Pause, null)
                            } else {
                                Icon(Icons.Default.PlayArrow, null)
                            }
                        }
                        FilledTonalIconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onNextClick,
                            content = { Icon(Icons.Default.SkipNext, null) }
                        )
                        FilledIconToggleButton(
                            modifier = Modifier.size(32.dp),
                            enabled = enabled,
                            checked = repeatState != RepeatState.Off,
                            onCheckedChange = { onRepeatClick() }
                        ) {
                            when (repeatState) {
                                RepeatState.Off -> Icon(Icons.Default.Repeat, null)
                                RepeatState.List -> Icon(Icons.Default.Repeat, null)
                                RepeatState.Track -> Icon(Icons.Default.RepeatOne, null)
                            }
                        }
                    }
                }
            }
        }

        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: PlayerState) {
                when (state) {
                    is PlayerState.Loading -> Unit
                    is PlayerState.Unavailable -> Unit
                    is PlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: PlayerState.Available) {
                Main(
                    modifier = modifier,
                    enabled = state.enabled,
                    track = state.track,
                    isQueueVisible = state.isQueueVisible,
                    isTrackVisible = state.isTrackVisible,
                    repeatState = state.repeatState,
                    queue = state.queue,
                    onToggleTrackVisibility = state.onToggleTrackVisibility,
                    onToggleQueueVisibility = state.onToggleQueueVisibility,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    onTrackNameClick = state.onTrackNameClick,
                    onArtistClick = state.onArtistClick,
                    onTimeChange = state.onTimeChange,
                    onPreviousClick = state.onPreviousClick,
                    onPlayClick = state.onPlayClick,
                    onPauseClick = state.onPauseClick,
                    onNextClick = state.onNextClick,
                    onRepeatClick = state.onRepeatClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onTrackNameClick: (TrackId) -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onTimeChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val isQueueVisible by isQueueVisible.collectAsState()
                val isTrackVisible by isTrackVisible.collectAsState()

                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTrackVisible) {
                                Track(
                                    modifier = Modifier.weight(1f),
                                    track = track,
                                    isQueueVisible = isQueueVisible,
                                    onTrackNameClick = { onTrackNameClick(track.id) },
                                    onArtistClick = onArtistClick
                                )
                            }
                            if (isQueueVisible) {
                                if (isTrackVisible) {
                                    queue(Modifier.weight(.45f))
                                } else {
                                    queue(Modifier.fillMaxWidth(.45f))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconToggleButton(
                                checked = isTrackVisible,
                                onCheckedChange = { onToggleTrackVisibility() },
                                content = { Icon(Icons.Default.ArtTrack, null) }
                            )
                            FilledIconToggleButton(
                                checked = isQueueVisible,
                                onCheckedChange = { onToggleQueueVisibility() },
                                content = { Icon(Icons.AutoMirrored.Default.QueueMusic, null) }
                            )
                        }
                        Common.PlaybackControls(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            track = track,
                            isPlaying = isPlaying,
                            repeatState = repeatState,
                            timePositionChange = timePositionChange,
                            onTimeChange = onTimeChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick,
                            onRepeatClick = onRepeatClick
                        )
                    }
                }
            }

            @Composable
            private fun Track(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem,
                isQueueVisible: Boolean,
                onTrackNameClick: () -> Unit,
                onArtistClick: (ArtistId) -> Unit
            ) {
                Surface(
                    modifier = modifier
                ) {
                    when (isQueueVisible) {
                        true -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    file = track.image
                                )
                                Common.TrackInfo(
                                    modifier = Modifier.padding(top = 12.dp),
                                    track = track,
                                    onTrackNameClick = onTrackNameClick,
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                        false -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    modifier = Modifier.weight(1f)
                                        .aspectRatio(1f, true),
                                    file = track.image
                                )
                                Common.TrackInfo(
                                    modifier = Modifier.weight(1f),
                                    track = track,
                                    onTrackNameClick = onTrackNameClick,
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                }
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: PlayerState) {
                when (state) {
                    is PlayerState.Loading -> Unit
                    is PlayerState.Unavailable -> Unit
                    is PlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: PlayerState.Available) {
                Main(
                    modifier = modifier,
                    enabled = state.enabled,
                    track = state.track,
                    isQueueVisible = state.isQueueVisible,
                    isTrackVisible = state.isTrackVisible,
                    repeatState = state.repeatState,
                    queue = state.queue,
                    onToggleTrackVisibility = state.onToggleTrackVisibility,
                    onToggleQueueVisibility = state.onToggleQueueVisibility,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    onTrackNameClick = state.onTrackNameClick,
                    onArtistClick = state.onArtistClick,
                    onTimeChange = state.onTimeChange,
                    onPreviousClick = state.onPreviousClick,
                    onPlayClick = state.onPlayClick,
                    onPauseClick = state.onPauseClick,
                    onNextClick = state.onNextClick,
                    onRepeatClick = state.onRepeatClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onTrackNameClick: (TrackId) -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onTimeChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val isQueueVisible by isQueueVisible.collectAsState()
                val isTrackVisible by isTrackVisible.collectAsState()

                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            if (isTrackVisible) {
                                Track(
                                    modifier = Modifier.fillMaxWidth(),
                                    track = track,
                                    onTrackNameClick = { onTrackNameClick(track.id) },
                                    onArtistClick = onArtistClick
                                )
                            } else if (isQueueVisible) {
                                queue(Modifier.fillMaxWidth())
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconToggleButton(
                                checked = isTrackVisible,
                                onCheckedChange = { onToggleTrackVisibility() },
                                content = { Icon(Icons.Default.ArtTrack, null) }
                            )
                            FilledIconToggleButton(
                                checked = isQueueVisible,
                                onCheckedChange = { onToggleQueueVisibility() },
                                content = { Icon(Icons.AutoMirrored.Default.QueueMusic, null) }
                            )
                        }
                        Common.PlaybackControls(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            track = track,
                            isPlaying = isPlaying,
                            repeatState = repeatState,
                            timePositionChange = timePositionChange,
                            onTimeChange = onTimeChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick,
                            onRepeatClick = onRepeatClick
                        )
                    }
                }
            }

            @Composable
            private fun Track(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem,
                onTrackNameClick: () -> Unit,
                onArtistClick: (ArtistId) -> Unit
            ) {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        modifier = Modifier.weight(1f)
                            .aspectRatio(1f),
                        file = track.image
                    )
                    Common.TrackInfo(
                        modifier = Modifier.padding(top = 12.dp),
                        track = track,
                        onTrackNameClick = onTrackNameClick,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}