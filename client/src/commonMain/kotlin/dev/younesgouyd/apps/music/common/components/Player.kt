package dev.younesgouyd.apps.music.common.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.components.util.AdaptiveUi
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.QueueItem
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.RepeatState
import dev.younesgouyd.apps.music.common.components.util.widgets.Image
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class Player(
    mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit,
    showQueue: () -> Unit,
    minimizePlayer: () -> Unit
) : Component() {
    override val title: String = "Player"
    private val state: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.Unavailable)

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
                        onAlbumClick = showAlbumDetails,
                        onArtistClick = showArtistDetails,
                        onShowQueueClick = showQueue,
                        onValueChange = mediaController::seek,
                        onPreviousClick = mediaController::previous,
                        onPlayClick = mediaController::play,
                        onPauseClick = mediaController::pause,
                        onNextClick = mediaController::next,
                        onRepeatClick = mediaController::repeat,
                        onPlayQueueItem = mediaController::playQueueItem,
                        onPlayQueueSubItem = mediaController::playTrackInQueue,
                        onMinimizeClick = minimizePlayer
                    )
                }
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        AdaptiveUi(
            wide = { Ui.Wide.Main(modifier = modifier, state = state) },
            compact = { Ui.Compact.Main(modifier = modifier, state = state) }
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
            val queue: List<QueueItem>,
            val queueItemIndex: Int,
            val queueSubItemIndex: Int,
            val timePositionChange: StateFlow<Long>,
            val isPlaying: StateFlow<Boolean>,
            val repeatState: RepeatState,
            val currentTrack: QueueItem.Track,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onShowQueueClick: () -> Unit,
            val onValueChange: (Long) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayClick: () -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onRepeatClick: () -> Unit,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) -> Unit,
            val onMinimizeClick: () -> Unit
        ) : PlayerState()
    }

    private object Ui {
        object Wide {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: PlayerState) {
                when (state) {
                    is PlayerState.Loading -> Unit
                    is PlayerState.Unavailable -> Unit
                    is PlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: PlayerState.Available) {
                Main(
                    modifier =  modifier,
                    enabled = state.enabled,
                    currentTrack = state.currentTrack,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    onMinimizeClick = state.onMinimizeClick,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onValueChange = state.onValueChange,
                    onPreviousClick = state.onPreviousClick,
                    onPlayClick = state.onPlayClick,
                    onPauseClick = state.onPauseClick,
                    onNextClick = state.onNextClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                currentTrack: MediaController.MediaControllerState.Available.QueueItem.Track,
                timePositionChange: StateFlow<Long>,
                isPlaying: StateFlow<Boolean>,
                onMinimizeClick: () -> Unit,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onValueChange: (Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit
            ) {
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.fillMaxWidth(0.4f),
                                data = currentTrack.album?.image,
                                contentScale = ContentScale.FillWidth
                            )
                            TrackInfo(
                                modifier = Modifier.weight(1f),
                                track = currentTrack,
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                            content = {
                                IconButton(
                                    modifier = Modifier.size(50.dp),
                                    onClick = onMinimizeClick
                                ) {
                                    Icon(
                                        modifier = Modifier.fillMaxSize(),
                                        imageVector = Icons.Default.UnfoldLess,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        PlaybackControls(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            track = currentTrack,
                            isPlaying = isPlaying,
                            timePositionChange = timePositionChange,
                            onValueChange = onValueChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick
                        )
                        Spacer(Modifier.size(14.dp))
                    }

                }
            }

            @Composable
            private fun TrackInfo(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit
            ) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    track.album?.let { album ->
                        TextButton(
                            onClick = { onAlbumClick(album.id) }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Album, null)
                                Text(
                                    text = album.name,
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
            private fun PlaybackControls(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                isPlaying: StateFlow<Boolean>,
                timePositionChange: StateFlow<Long>,
                onValueChange: (Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit
            ) {
                fun <T> linearAnimation(duration: Int): TweenSpec<T> = tween(durationMillis = duration, easing = LinearEasing)
                fun durationMillisFormatted(time: Long): String = time.milliseconds.toComponents { minutes, seconds, _ -> minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0') }

                val enabled by enabled.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val animatedPosition = remember { Animatable(0f) }
                val formattedDuration = remember(track.duration) { durationMillisFormatted(track.duration) }
                val isUserInteracting = remember { mutableStateOf(false) }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlaybackSlider(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = enabled,
                        duration = track.duration,
                        animatedPosition = animatedPosition,
                        onSeek = onValueChange,
                        isInteracting = isUserInteracting
                    )
                    Text(
                        text = "${durationMillisFormatted((animatedPosition.value * track.duration).toLong())}/${formattedDuration}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = false, // TODO
                            onClick = {} // TODO
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onPreviousClick
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null
                            )
                        }
                        when (isPlaying) {
                            true -> IconButton(
                                modifier = Modifier.size(70.dp),
                                enabled = enabled,
                                onClick = onPauseClick
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null
                                )
                            }
                            false -> IconButton(
                                modifier = Modifier.size(70.dp),
                                enabled = enabled,
                                onClick = onPlayClick
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onNextClick
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = false, // TODO
                            onClick = {} // TODO
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null
                            )
                        }
                    }
                }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        val remaining = 1f - animatedPosition.value
                        val remainingDuration = (remaining * track.duration).toInt()
                        animatedPosition.animateTo(targetValue = 1f, animationSpec = linearAnimation(remainingDuration))
                    } else {
                        animatedPosition.stop()
                    }
                }

                LaunchedEffect(timePositionChange) {
                    if (!isUserInteracting.value) {
                        animatedPosition.stop()
                        animatedPosition.snapTo(timePositionChange.toFloat() / track.duration.toFloat())
                        if (isPlaying) {
                            val remaining = track.duration - timePositionChange
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(remaining.toInt())
                            )
                        }
                    }
                }
            }

            @Composable
            private fun PlaybackSlider(
                modifier: Modifier = Modifier,
                duration: Long,
                animatedPosition: Animatable<Float, AnimationVector1D>,
                enabled: Boolean,
                onSeek: (Long) -> Unit,
                isInteracting: MutableState<Boolean>
            ) {
                var sliderPosition by remember { mutableFloatStateOf(0f) }
                val sliderValue = if (isInteracting.value) sliderPosition else animatedPosition.value

                Slider(
                    modifier = modifier,
                    enabled = enabled,
                    value = sliderValue,
                    onValueChange = { newValue ->
                        isInteracting.value = true
                        sliderPosition = newValue
                    },
                    onValueChangeFinished = {
                        isInteracting.value = false
                        onSeek((sliderPosition * duration).toLong())
                    }
                )
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: PlayerState) {
                when (state) {
                    is PlayerState.Loading -> Unit
                    is PlayerState.Unavailable -> Unit
                    is PlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: PlayerState.Available) {
                Main(
                    modifier =  modifier,
                    enabled = state.enabled,
                    currentTrack = state.currentTrack,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onShowQueueClick = state.onShowQueueClick,
                    onValueChange = state.onValueChange,
                    onPreviousClick = state.onPreviousClick,
                    onPlayClick = state.onPlayClick,
                    onPauseClick = state.onPauseClick,
                    onNextClick = state.onNextClick,
                    onMinimizeClick = state.onMinimizeClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                currentTrack: MediaController.MediaControllerState.Available.QueueItem.Track,
                timePositionChange: StateFlow<Long>,
                isPlaying: StateFlow<Boolean>,
                onShowQueueClick: () -> Unit,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onValueChange: (Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onMinimizeClick: () -> Unit
            ) {
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
                        Image(
                            modifier = Modifier.fillMaxWidth(),
                            data = currentTrack.album?.image,
                            contentScale = ContentScale.FillWidth
                        )
                        TrackInfo(
                            modifier = Modifier.fillMaxWidth(),
                            track = currentTrack,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick
                        )
                        PlaybackControls(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            track = currentTrack,
                            isPlaying = isPlaying,
                            timePositionChange = timePositionChange,
                            onShowQueueClick = onShowQueueClick,
                            onValueChange = onValueChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick
                        )
                        IconButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onMinimizeClick,
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                }
            }

            @Composable
            private fun TrackInfo(
                modifier: Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit
            ) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    track.album?.let { album ->
                        TextButton(
                            onClick = { onAlbumClick(album.id) }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Album, null)
                                Text(
                                    text = album.name,
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
            private fun PlaybackControls(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                isPlaying: StateFlow<Boolean>,
                timePositionChange: StateFlow<Long>,
                onShowQueueClick: () -> Unit,
                onValueChange: (Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit
            ) {
                fun <T> linearAnimation(duration: Int): TweenSpec<T> = tween(durationMillis = duration, easing = LinearEasing)
                fun durationMillisFormatted(time: Long): String = time.milliseconds.toComponents { minutes, seconds, _ -> minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0') }

                val enabled by enabled.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val animatedPosition = remember { Animatable(0f) }
                val formattedDuration = remember(track.duration) { durationMillisFormatted(track.duration) }
                val isUserInteracting = remember { mutableStateOf(false) }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onShowQueueClick,
                            content = { Icon(Icons.AutoMirrored.Default.QueueMusic, null) }
                        )
                    }
                    PlaybackSlider(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = enabled,
                        duration = track.duration,
                        animatedPosition = animatedPosition,
                        onSeek = onValueChange,
                        isInteracting = isUserInteracting
                    )
                    Text(
                        text = "${durationMillisFormatted((animatedPosition.value * track.duration).toLong())}/${formattedDuration}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = false, // TODO
                            onClick = {} // TODO
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onPreviousClick
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null
                            )
                        }
                        when (isPlaying) {
                            true -> IconButton(
                                modifier = Modifier.size(70.dp),
                                enabled = enabled,
                                onClick = onPauseClick
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null
                                )
                            }
                            false -> IconButton(
                                modifier = Modifier.size(70.dp),
                                enabled = enabled,
                                onClick = onPlayClick
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = enabled,
                            onClick = onNextClick
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            enabled = false, // TODO
                            onClick = {} // TODO
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null
                            )
                        }
                    }
                }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        val remaining = 1f - animatedPosition.value
                        val remainingDuration = (remaining * track.duration).toInt()
                        animatedPosition.animateTo(targetValue = 1f, animationSpec = linearAnimation(remainingDuration))
                    } else {
                        animatedPosition.stop()
                    }
                }

                LaunchedEffect(timePositionChange) {
                    if (!isUserInteracting.value) {
                        animatedPosition.stop()
                        animatedPosition.snapTo(timePositionChange.toFloat() / track.duration.toFloat())
                        if (isPlaying) {
                            val remaining = track.duration - timePositionChange
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(remaining.toInt())
                            )
                        }
                    }
                }
            }

            @Composable
            private fun PlaybackSlider(
                modifier: Modifier = Modifier,
                duration: Long,
                animatedPosition: Animatable<Float, AnimationVector1D>,
                enabled: Boolean,
                onSeek: (Long) -> Unit,
                isInteracting: MutableState<Boolean>
            ) {
                var sliderPosition by remember { mutableFloatStateOf(0f) }
                val sliderValue = if (isInteracting.value) sliderPosition else animatedPosition.value

                Slider(
                    modifier = modifier,
                    enabled = enabled,
                    value = sliderValue,
                    onValueChange = { newValue ->
                        isInteracting.value = true
                        sliderPosition = newValue
                    },
                    onValueChangeFinished = {
                        isInteracting.value = false
                        onSeek((sliderPosition * duration).toLong())
                    }
                )
            }
        }
    }
}