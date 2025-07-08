package dev.younesgouyd.apps.music.desktop.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.components.Player
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.QueueItem
import dev.younesgouyd.apps.music.common.components.util.MediaController.MediaControllerState.Available.RepeatState
import dev.younesgouyd.apps.music.desktop.components.util.widgets.Image
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

class Player(
    mediaController: MediaController
) : Player(mediaController) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
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
                repeatState = state.repeatState,
                onAlbumClick = state.onAlbumClick,
                onArtistClick = state.onArtistClick,
                onValueChange = state.onValueChange,
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
            currentTrack: QueueItem.Track,
            timePositionChange: StateFlow<Long>,
            isPlaying : StateFlow<Boolean>,
            repeatState: RepeatState,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onValueChange: (Long) -> Unit,
            onPreviousClick: () -> Unit,
            onPlayClick: () -> Unit,
            onPauseClick: () -> Unit,
            onNextClick: () -> Unit,
            onRepeatClick: () -> Unit
        ) {
            fun <T> linearAnimation(duration: Int): TweenSpec<T> = tween(durationMillis = duration, easing = LinearEasing)
            fun durationMillisFormatted(time: Long): String = time.milliseconds.toComponents { minutes, seconds, _ -> minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0') }

            val enabled by enabled.collectAsState()
            val isPlaying by isPlaying.collectAsState()
            val timePositionChange by timePositionChange.collectAsState()
            val animatedPosition = remember { Animatable(0f) }
            val formattedDuration = remember(currentTrack.duration) { durationMillisFormatted(currentTrack.duration) }
            val isUserInteracting = remember { mutableStateOf(false) }

            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        data = currentTrack.album?.image
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTrack.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                content = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Album, null)
                                        Text(
                                            text = currentTrack.album?.name ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = { currentTrack.album?.let { onAlbumClick(it.id) } }
                            )
                            currentTrack.artists.firstOrNull()?.let { artist ->
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
                                                RepeatState.Off -> Icon(Icons.Default.Repeat, null)
                                                RepeatState.Track -> Icon(Icons.Default.RepeatOneOn, null)
                                                RepeatState.List -> Icon(Icons.Default.RepeatOn, null)
                                            }

                                        }
                                    )
                                }
                            }
                            PlaybackSlider(
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                duration = currentTrack.duration,
                                animatedPosition = animatedPosition,
                                onSeek = onValueChange,
                                isInteracting = isUserInteracting
                            )
//                            Text(
//                                text = "${durationMillisFormatted((animatedPosition.value * currentTrack.duration).toLong())}/${formattedDuration}",
//                                style = MaterialTheme.typography.labelMedium
//                            )
                        }
                    }
                }
            }

            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    val remaining = 1f - animatedPosition.value
                    val remainingDuration = (remaining * currentTrack.duration).toInt()
                    animatedPosition.animateTo(targetValue = 1f, animationSpec = linearAnimation(remainingDuration))
                } else {
                    animatedPosition.stop()
                }
            }

            LaunchedEffect(timePositionChange) {
                if (!isUserInteracting.value) {
                    animatedPosition.stop()
                    animatedPosition.snapTo(timePositionChange.toFloat() / currentTrack.duration.toFloat().toFloat())
                    if (isPlaying) {
                        val remaining = currentTrack.duration - timePositionChange
                        animatedPosition.animateTo(
                            targetValue = 1f,
                            animationSpec = linearAnimation(remaining.toInt())
                        )
                    }
                }
            }
        }

        @Composable
        fun PlaybackSlider(
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