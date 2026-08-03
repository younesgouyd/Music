package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.MediaController
import dev.younesgouyd.apps.music.client.common.components.util.*
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class Player(
    mediaController: MediaController,
    showTack: (TrackId) -> Unit,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit,
    queue: @Composable (Modifier) -> Unit
) : Component() {
    override val title: String = "Player"
    private var windowSizeClass: WindowWidthSizeClass? = null
    private val state: StateFlow<Ui.State>

    init {
        val isTrackVisible = MutableStateFlow(true)
        val isQueueVisible = MutableStateFlow(false)

        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> Ui.State.Unavailable
                is MediaController.MediaControllerState.Loading -> Ui.State.Loading
                is MediaController.MediaControllerState.Available -> Ui.State.Available(
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
                            WindowWidthSizeClass.Compact -> {
                                isTrackVisible.update {
                                    if (!it) { isQueueVisible.value = false }
                                    !it
                                }
                            }
                            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> isTrackVisible.update { !it }
                            null -> TODO()
                        }
                    },
                    onToggleQueueVisibility = {
                        when (windowSizeClass) {
                            WindowWidthSizeClass.Compact -> {
                                isQueueVisible.update {
                                    if (!it) { isTrackVisible.value = false }
                                    !it
                                }
                            }
                            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> isQueueVisible.update { !it }
                            null -> TODO()
                        }
                    },
                    onTrackNameClick = showTack,
                    onAlbumClick = showAlbum,
                    onArtistClick = showArtist,
                    onTimeChange = mediaController::seek,
                    onPreviousClick = mediaController::previous,
                    onPlayClick = mediaController::play,
                    onPauseClick = mediaController::pause,
                    onNextClick = mediaController::next,
                    onRepeatClick = mediaController::repeat
                )
            }
        }.stateIn(coroutineScope, SharingStarted.Lazily, Ui.State.Unavailable)
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            data object Unavailable : State()

            data class Available(
                val enabled: StateFlow<Boolean>,
                val timePositionChange: StateFlow<Duration>,
                val isPlaying: StateFlow<Boolean>,
                val track: StateFlow<MediaController.MediaControllerState.Available.QueueItem?>,
                val isTrackVisible: StateFlow<Boolean>,
                val isQueueVisible: StateFlow<Boolean>,
                val repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
                val queue: @Composable (Modifier) -> Unit,
                val onToggleQueueVisibility: () -> Unit,
                val onToggleTrackVisibility: () -> Unit,
                val onTrackNameClick: (TrackId) -> Unit,
                val onAlbumClick: (SpotifyAlbumId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onTimeChange: (Duration) -> Unit,
                val onPreviousClick: () -> Unit,
                val onPlayClick: () -> Unit,
                val onPauseClick: () -> Unit,
                val onNextClick: () -> Unit,
                val onRepeatClick: () -> Unit
            ) : State()
        }

        private object Common {
            @Composable
            fun TrackInfo(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem?,
                onTrackNameClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit
            ) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onTrackNameClick,
                        enabled = track != null
                    ) {
                        Text(
                            text = track?.name ?: "",
                            style = MaterialTheme.typography.displayMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    when (track?.album) {
                        is MediaController.MediaControllerState.Available.QueueItem.Album.ImportAlbum -> {
                            Album(
                                name = track.album.name ?: ""
                            )
                        }
                        is MediaController.MediaControllerState.Available.QueueItem.Album.SpotifyAlbum -> {
                            Album(
                                name = track.album.name,
                                onClick = { onAlbumClick(track.album.id) })
                        }
                        null -> {
                            Album(
                                name = ""
                            )
                        }
                    }
                    when (track?.artists) {
                        is MediaController.MediaControllerState.Available.QueueItem.Artists.ImportArtist -> {
                            Artists(
                                names = track.artists.list
                            )
                        }
                        is MediaController.MediaControllerState.Available.QueueItem.Artists.SpotifyArtists -> {
                            Artists(
                                artists = track.artists.list,
                                onArtistClick = onArtistClick
                            )
                        }
                        null -> {
                            Artists(
                                names = emptyList()
                            )
                        }
                    }
                }
            }

            @Composable
            fun PlaybackControls(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: MediaController.MediaControllerState.Available.QueueItem?,
                isPlaying: StateFlow<Boolean>,
                repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
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
                val formattedDuration = remember(track?.duration) { track?.duration.formatted() }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlaybackSlider(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = enabled,
                        duration = track?.duration,
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
                            checked = repeatState != MediaController.MediaControllerState.Available.RepeatState.Off,
                            onCheckedChange = { onRepeatClick() }
                        ) {
                            when (repeatState) {
                                MediaController.MediaControllerState.Available.RepeatState.Off -> Icon(Icons.Default.Repeat, null)
                                MediaController.MediaControllerState.Available.RepeatState.List -> Icon(Icons.Default.Repeat, null)
                                MediaController.MediaControllerState.Available.RepeatState.Track -> Icon(Icons.Default.RepeatOne, null)
                            }
                        }
                    }
                }
            }
        }

        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Unit
                    is State.Unavailable -> Unit
                    is State.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: State.Available) {
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
                    onAlbumClick = state.onAlbumClick,
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
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem?>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onTrackNameClick: (TrackId) -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
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
                                    onTrackNameClick = { onTrackNameClick(track!!.id) }, // TODO
                                    onAlbumClick = onAlbumClick,
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
                track: MediaController.MediaControllerState.Available.QueueItem?,
                isQueueVisible: Boolean,
                onTrackNameClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit
            ) {
                Surface(modifier) {
                    when (isQueueVisible) {
                        true -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    file = track?.image
                                )
                                Common.TrackInfo(
                                    modifier = Modifier.padding(top = 12.dp),
                                    track = track,
                                    onTrackNameClick = onTrackNameClick,
                                    onAlbumClick = onAlbumClick,
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
                                    file = track?.image
                                )
                                Common.TrackInfo(
                                    modifier = Modifier.weight(1f),
                                    track = track,
                                    onTrackNameClick = onTrackNameClick,
                                    onAlbumClick = onAlbumClick,
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
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Unit
                    is State.Unavailable -> Unit
                    is State.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: State.Available) {
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
                    onAlbumClick = state.onAlbumClick,
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
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem?>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onTrackNameClick: (TrackId) -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
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
                                    onTrackNameClick = { onTrackNameClick(track!!.id) }, // TODO
                                    onAlbumClick = onAlbumClick,
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
                track: MediaController.MediaControllerState.Available.QueueItem?,
                onTrackNameClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit
            ) {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        file = track?.image
                    )
                    Common.TrackInfo(
                        modifier = Modifier.padding(top = 12.dp),
                        track = track,
                        onTrackNameClick = onTrackNameClick,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}