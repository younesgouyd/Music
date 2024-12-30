package dev.younesgouyd.apps.music.app.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Player(
    private val mediaController: MediaController
) : Component() {
    override val title: String = ""
    private val state: StateFlow<MediaController.MediaControllerState>
        get() = mediaController.state

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
        fun Main(modifier: Modifier = Modifier, state: MediaController.MediaControllerState) {
            when (state) {
                is MediaController.MediaControllerState.Loading -> Unit
                is MediaController.MediaControllerState.Unavailable -> Unit
                is MediaController.MediaControllerState.Available -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier = Modifier, state: MediaController.MediaControllerState.Available) {
            Main(
                modifier =  modifier,
                enabled = state.enabled,
                playbackState = state.playbackState,
                onAlbumClick = state.onAlbumClick,
                onArtistClick = state.onArtistClick,
                onValueChange = state.onValueChange,
                onPreviousClick = state.onPreviousClick,
                onPlayClick = { state.onPlayClick(emptyList()) },
                onPauseClick = state.onPauseClick,
                onNextClick = state.onNextClick,
                onRepeatClick = state.onRepeatClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            enabled: StateFlow<Boolean>,
            playbackState: MediaController.MediaControllerState.Available.PlaybackState,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onValueChange: (Duration) -> Unit,
            onPreviousClick: () -> Unit,
            onPlayClick: () -> Unit,
            onPauseClick: () -> Unit,
            onNextClick: () -> Unit,
            onRepeatClick: () -> Unit
        ) {
            val duration: Duration = playbackState.duration
            val animateableElapsedTime = remember { animateableOf(0.milliseconds) }

            _Main(
                modifier = modifier,
                enabled = enabled,
                playbackState = playbackState.copy(elapsedTime = animateableElapsedTime.value),
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onValueChange = onValueChange,
                onPreviousClick = onPreviousClick,
                onPlayClick = onPlayClick,
                onPauseClick = onPauseClick,
                onNextClick = onNextClick,
                onRepeatClick = onRepeatClick
            )

            LaunchedEffect(duration) {
                animateableElapsedTime.stop()
                animateableElapsedTime.snapTo(0.milliseconds)
                animateableElapsedTime.updateBounds(lowerBound = 0.milliseconds, upperBound = duration)
                if (playbackState.isPlaying) {
                    animateableElapsedTime.animateTo(
                        targetValue = duration,
                        animationSpec = linearAnimation(duration)
                    )
                }
            }

            LaunchedEffect(playbackState.isPlaying) {
                if (playbackState.isPlaying) {
                    animateableElapsedTime.animateTo(
                        targetValue = duration,
                        animationSpec = linearAnimation(duration - animateableElapsedTime.value)
                    )
                } else {
                    animateableElapsedTime.stop()
                }
            }

            LaunchedEffect(playbackState.elapsedTime, playbackState.isPlaying) {
                animateableElapsedTime.snapTo(playbackState.elapsedTime)
                if (playbackState.isPlaying) {
                    animateableElapsedTime.animateTo(
                        targetValue = duration,
                        animationSpec = linearAnimation(duration - playbackState.elapsedTime)
                    )
                }
            }
        }

        @Composable
        private fun _Main(
            modifier: Modifier = Modifier,
            enabled: StateFlow<Boolean>,
            playbackState: MediaController.MediaControllerState.Available.PlaybackState,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onValueChange: (Duration) -> Unit,
            onPreviousClick: () -> Unit,
            onPlayClick: () -> Unit,
            onPauseClick: () -> Unit,
            onNextClick: () -> Unit,
            onRepeatClick: () -> Unit
        ) {
            val enabled by enabled.collectAsState()
            val duration: Duration = playbackState.duration

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
                        modifier = Modifier.size(170.dp),
                        data = playbackState.currentTrack.album?.image
                    )
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = playbackState.currentTrack.name,
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
                                        text = playbackState.currentTrack.album?.name ?: "",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = { playbackState.currentTrack.album?.let { onAlbumClick(it.id) } }
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (artist in playbackState.currentTrack.artists) {
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
                            Text(
                                text = "${playbackState.elapsedTime.formatted()}/${duration.formatted()}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                content = { Icon(Icons.Default.SkipPrevious, null) },
                                enabled = enabled,
                                onClick = onPreviousClick
                            )
                            when (playbackState.isPlaying) {
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
                                    when (playbackState.repeatState) {
                                        MediaController.MediaControllerState.Available.PlaybackState.RepeatState.Off -> Icon(Icons.Default.Repeat, null)
                                        MediaController.MediaControllerState.Available.PlaybackState.RepeatState.Track -> Icon(Icons.Default.RepeatOneOn, null)
                                        MediaController.MediaControllerState.Available.PlaybackState.RepeatState.List -> Icon(Icons.Default.RepeatOn, null)
                                    }

                                }
                            )
                            Slider(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = enabled,
                                value = playbackState.elapsedTime.inWholeMilliseconds.toFloat(),
                                valueRange = 0f..duration.inWholeMilliseconds.toFloat(),
                                onValueChange = { onValueChange(it.toLong().milliseconds) }
                            )
                        }
                    }
                }
            }
        }

        private fun animateableOf(
            initialValue: Duration
        ): Animatable<Duration, AnimationVector1D> {
            return Animatable(
                initialValue = initialValue,
                typeConverter = TwoWayConverter(
                    convertToVector = { AnimationVector1D(it.inWholeMilliseconds.toFloat()) },
                    convertFromVector = { it.value.toLong().milliseconds }
                )
            )
        }

        private fun linearAnimation(duration: Duration): TweenSpec<Duration> {
            return tween(
                durationMillis = duration.inWholeMilliseconds.toInt(),
                easing = LinearEasing
            )
        }

        fun Duration?.formatted(): String {
            return this?.let {
                it.toComponents { minutes, seconds, _ ->
                    minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
                }
            } ?: ""
        }
    }
}