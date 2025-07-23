package dev.younesgouyd.apps.music.android.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.android.components.util.widgets.Image
import dev.younesgouyd.apps.music.common.components.MiniPlayer
import dev.younesgouyd.apps.music.common.components.util.MediaController
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

class MiniPlayer(
    mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit
) : MiniPlayer(mediaController, showAlbumDetails, showArtistDetails) {
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
                modifier =  modifier,
                currentTrack = state.currentTrack,
                timePositionChange = state.timePositionChange,
                isPlaying = state.isPlaying
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            currentTrack: MediaController.MediaControllerState.Available.QueueItem.Track,
            timePositionChange: StateFlow<Long>,
            isPlaying: StateFlow<Boolean>
        ) {
            fun <T> linearAnimation(duration: Int): TweenSpec<T> = tween(durationMillis = duration, easing = LinearEasing)
            fun durationMillisFormatted(time: Long): String = time.milliseconds.toComponents { minutes, seconds, _ -> minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0') }

            val isPlaying by isPlaying.collectAsState()
            val timePositionChange by timePositionChange.collectAsState()
            val animatedPosition = remember { Animatable(0f) }
            val formattedDuration = remember(currentTrack.duration) { durationMillisFormatted(currentTrack.duration) }

            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.fillMaxHeight(),
                        data = currentTrack.album?.image,
                        contentScale = ContentScale.FillHeight
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentTrack.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentTrack.artists.firstOrNull()?.let { artist ->
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
                                text = "${durationMillisFormatted((animatedPosition.value * currentTrack.duration).toLong())}/${formattedDuration}",
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
                if (isPlaying) {
                    val remaining = 1f - animatedPosition.value
                    val remainingDuration = (remaining * currentTrack.duration).toInt()
                    animatedPosition.animateTo(targetValue = 1f, animationSpec = linearAnimation(remainingDuration))
                } else {
                    animatedPosition.stop()
                }
            }

            LaunchedEffect(timePositionChange) {
                animatedPosition.stop()
                animatedPosition.snapTo(timePositionChange.toFloat() / currentTrack.duration.toFloat())
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
}