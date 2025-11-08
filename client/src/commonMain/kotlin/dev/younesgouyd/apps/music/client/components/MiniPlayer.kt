package dev.younesgouyd.apps.music.client.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.client.components.util.compose.PlaybackSlider
import dev.younesgouyd.apps.music.client.components.util.compose.formatted
import dev.younesgouyd.apps.music.client.components.util.compose.linearAnimation
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Image
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MiniPlayer(
    mediaController: MediaController,
    showArtistDetails: (Long) -> Unit,
    expand: () -> Unit
) : dev.younesgouyd.apps.music.client.util.Component() {
    override val title: String = "Mini Player"
    private val state: MutableStateFlow<MiniPlayerState> = MutableStateFlow(MiniPlayerState.Unavailable)

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
                        track = mediaControllerState.track,
                        onClick = expand,
                        onArtistClick = showArtistDetails,
                        onTimeChange = mediaController::seek,
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

    private sealed class MiniPlayerState {
        data object Loading : MiniPlayerState()

        data object Unavailable : MiniPlayerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val timePositionChange: StateFlow<Duration>,
            val isPlaying: StateFlow<Boolean>,
            val repeatState: MediaController.MediaControllerState.Available.RepeatState,
            val track: MediaController.MediaControllerState.Available.QueueItem.Track,
            val onClick: () -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onTimeChange: (Duration) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayClick: () -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onRepeatClick: () -> Unit
        ) : MiniPlayerState()
    }

    private object Ui {
        private object Common

        object Wide {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: MiniPlayerState) {
                when (state) {
                    is MiniPlayerState.Loading -> Unit
                    is MiniPlayerState.Unavailable -> Unit
                    is MiniPlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: MiniPlayerState.Available) {
                Main(
                    modifier = modifier,
                    enabled = state.enabled,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    repeatState = state.repeatState,
                    track = state.track,
                    onClick = state.onClick,
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
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                repeatState: MediaController.MediaControllerState.Available.RepeatState,
                onClick: () -> Unit,
                onArtistClick: (Long) -> Unit,
                onTimeChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val animatedPosition = remember { Animatable(0f) }
                val formattedDuration = remember(track.duration) { track.duration.formatted() }
                val isUserInteracting = remember { mutableStateOf(false) }

                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    onClick = onClick
                ) {
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.fillMaxHeight(),
                            data = track.image,
                            contentScale = ContentScale.FillHeight
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Album, null)
                                    Text(
                                        text = track.album ?: "",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(track.artists) { artist ->
                                    TextButton(
                                        content = {
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
                                        },
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, null) },
                                            onClick = { TODO() }
                                        )
                                        IconButton(
                                            content = { Icon(Icons.Default.Folder, null) },
                                            onClick = { TODO() }
                                        )
                                    }
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            content = { Icon(Icons.Default.SkipPrevious, null) },
                                            enabled = enabled,
                                            onClick = onPreviousClick
                                        )
                                        when (isPlaying) {
                                            true -> IconButton(
                                                content = { Icon(Icons.Default.PauseCircle, null) },
                                                enabled = enabled,
                                                onClick = onPauseClick
                                            )

                                            false -> IconButton(
                                                content = { Icon(Icons.Default.PlayCircle, null) },
                                                enabled = enabled,
                                                onClick = onPlayClick
                                            )
                                        }
                                        IconButton(
                                            content = { Icon(Icons.Default.SkipNext, null) },
                                            enabled = enabled,
                                            onClick = onNextClick
                                        )
                                        IconButton(
                                            enabled = enabled,
                                            onClick = onRepeatClick,
                                            content = {
                                                when (repeatState) {
                                                    MediaController.MediaControllerState.Available.RepeatState.Off -> Icon(
                                                        Icons.Default.Repeat,
                                                        null
                                                    )

                                                    MediaController.MediaControllerState.Available.RepeatState.Track -> Icon(
                                                        Icons.Default.RepeatOneOn,
                                                        null
                                                    )

                                                    MediaController.MediaControllerState.Available.RepeatState.List -> Icon(
                                                        Icons.Default.RepeatOn,
                                                        null
                                                    )
                                                }

                                            }
                                        )
                                    }
                                }
                                PlaybackSlider(
                                    modifier = Modifier.weight(1f),
                                    enabled = enabled,
                                    duration = track.duration,
                                    animatedPosition = animatedPosition,
                                    onSeek = onTimeChange,
                                    isInteracting = isUserInteracting
                                )
                                Text(
                                    text = "${
                                        track.duration?.inWholeMilliseconds?.let { (animatedPosition.value * it) }
                                            ?.toLong()?.milliseconds.formatted()
                                    }/${formattedDuration}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                }

                LaunchedEffect(isPlaying) {
                    if (track.duration != null) {
                        if (isPlaying) {
                            val remaining = 1f - animatedPosition.value
                            val remainingDuration: Duration =
                                (remaining * track.duration.inWholeMilliseconds).toLong().milliseconds
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(remainingDuration)
                            )
                        } else {
                            animatedPosition.stop()
                        }
                    }
                }

                LaunchedEffect(timePositionChange) {
                    if (!isUserInteracting.value && track.duration != null) {
                        animatedPosition.stop()
                        animatedPosition.snapTo(
                            timePositionChange.inWholeMilliseconds.toFloat() / track.duration.inWholeMilliseconds.toFloat()
                        )
                        if (isPlaying) {
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(track.duration - timePositionChange)
                            )
                        }
                    }
                }
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: MiniPlayerState) {
                when (state) {
                    is MiniPlayerState.Loading -> Unit
                    is MiniPlayerState.Unavailable -> Unit
                    is MiniPlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: MiniPlayerState.Available) {
                Main(
                    modifier = modifier,
                    track = state.track,
                    timePositionChange = state.timePositionChange,
                    isPlaying = state.isPlaying,
                    onClick = state.onClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                track: MediaController.MediaControllerState.Available.QueueItem.Track,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onClick: () -> Unit
            ) {
                val isPlaying by isPlaying.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val animatedPosition = remember { Animatable(0f) }
                val formattedDuration = remember(track.duration) { track.duration.formatted() }

                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    onClick = onClick
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.fillMaxHeight(),
                            data = track.image,
                            contentScale = ContentScale.FillHeight
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                track.artists.firstOrNull()?.let { artist ->
                                    Row(
                                        modifier = Modifier.weight(1f),
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
                                Text(
                                    text = "${
                                        track.duration?.inWholeMilliseconds?.let { (animatedPosition.value * it) }
                                            ?.toLong()?.milliseconds.formatted()
                                    }/${formattedDuration}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { animatedPosition.value }
                            )
                        }
                        Spacer(Modifier.wrapContentWidth().size(4.dp))
                    }
                }

                LaunchedEffect(isPlaying) {
                    if (track.duration != null) {
                        if (isPlaying) {
                            val remaining = 1f - animatedPosition.value
                            val remainingDuration =
                                (remaining * track.duration.inWholeMilliseconds).toLong().milliseconds
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(remainingDuration)
                            )
                        } else {
                            animatedPosition.stop()
                        }
                    }
                }

                LaunchedEffect(timePositionChange) {
                    if (track.duration != null) {
                        animatedPosition.stop()
                        animatedPosition.snapTo(timePositionChange.inWholeMilliseconds.toFloat() / track.duration.inWholeMilliseconds.toFloat())
                        if (isPlaying) {
                            animatedPosition.animateTo(
                                targetValue = 1f,
                                animationSpec = linearAnimation(track.duration - timePositionChange)
                            )
                        }
                    }
                }
            }
        }
    }
}