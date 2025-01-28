package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
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
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier =  modifier,
                enabled = state.enabled,
                playbackState = state.playbackState,
                onAlbumClick = state.onAlbumClick,
                onArtistClick = state.onArtistClick,
                onValueChange = state.onValueChange,
                onAddToPlaylistClick = { state.onAddToPlaylistClick(state.playbackState.currentTrack.id) } ,
                onPreviousClick = state.onPreviousClick,
                onPlayClick = state.onPlayClick,
                onPauseClick = state.onPauseClick,
                onNextClick = state.onNextClick,
                onRepeatClick = state.onRepeatClick
            )

            if (addToPlaylistDialogVisible) {
                Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                    addToPlaylist!!.show(Modifier)
                }
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            enabled: StateFlow<Boolean>,
            playbackState: MediaController.MediaControllerState.Available.PlaybackState,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onValueChange: (Long) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onPreviousClick: () -> Unit,
            onPlayClick: () -> Unit,
            onPauseClick: () -> Unit,
            onNextClick: () -> Unit,
            onRepeatClick: () -> Unit
        ) {
            val enabled by enabled.collectAsState()
            val duration: Long = playbackState.duration
            val elapsedTime by playbackState.elapsedTime.collectAsState()

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
                                        onClick = onAddToPlaylistClick
                                    )
                                    IconButton(
                                        content = { Icon(Icons.Default.Folder, null) },
                                        onClick = { /* TODO: onAddToFolderClick */ }
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
                                }
                            }
                            Slider(
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                value = elapsedTime.toFloat(),
                                valueRange = 0f..duration.toFloat(),
                                onValueChange = { onValueChange(it.toLong()) }
                            )
                            Text(
                                text = "${durationMillisFormatted(elapsedTime)}/${durationMillisFormatted(duration)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        fun durationMillisFormatted(time: Long): String {
            return time.let {
                it.milliseconds.toComponents { minutes, seconds, _ ->
                    minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
                }
            }
        }
    }
}