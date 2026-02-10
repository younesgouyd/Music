package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class MiniPlayer(
    mediaController: MediaController,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit,
    expand: () -> Unit
) : Component() {
    override val title: String = "Mini Player"
    private val state: StateFlow<MiniPlayerState>

    init {
        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> MiniPlayerState.Unavailable
                is MediaController.MediaControllerState.Loading -> MiniPlayerState.Loading
                is MediaController.MediaControllerState.Available -> MiniPlayerState.Available(
                    enabled = mediaControllerState.enabled,
                    timePositionChange = mediaControllerState.timePositionChange,
                    isPlaying = mediaControllerState.isPlaying,
                    repeatState = mediaControllerState.repeatState,
                    track = mediaControllerState.currentItem,
                    onClick = expand,
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
        }.stateIn(coroutineScope, SharingStarted.Lazily, MiniPlayerState.Unavailable)
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
            val repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
            val track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
            val onClick: () -> Unit,
            val onAlbumClick: (SpotifyAlbumId) -> Unit,
            val onArtistClick: (SpotifyArtistId) -> Unit,
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
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                repeatState: StateFlow<MediaController.MediaControllerState.Available.RepeatState>,
                onClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onTimeChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val track by track.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val repeatState by repeatState.collectAsState()
                val formattedDuration = remember(track.duration) { track.duration.formatted() }

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
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            file = track.image
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
                            track.album.let { album ->
                                when (album) {
                                    is MediaController.MediaControllerState.Available.QueueItem.Album.ImportAlbum -> {
                                        Album(name = album.name ?: "")
                                    }
                                    is MediaController.MediaControllerState.Available.QueueItem.Album.SpotifyAlbum -> {
                                        Album(name = album.name, onClick = { onAlbumClick(album.id) })
                                    }
                                }
                            }
                            track.artists.let { artists ->
                                when (artists) {
                                    is MediaController.MediaControllerState.Available.QueueItem.Artists.ImportArtist -> {
                                        Artists(names = artists.list)
                                    }
                                    is MediaController.MediaControllerState.Available.QueueItem.Artists.SpotifyArtists -> {
                                        Artists(artists = artists.list, onArtistClick = onArtistClick)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
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
                                    shape = MaterialTheme.shapes.small,
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
                                    currentPosition = timePositionChange,
                                    onSeek = onTimeChange
                                )
                                Text(
                                    text = "${timePositionChange.formatted()}/${formattedDuration}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Spacer(Modifier.size(8.dp))
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
                track: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
                timePositionChange: StateFlow<Duration>,
                isPlaying: StateFlow<Boolean>,
                onClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val timePositionChange by timePositionChange.collectAsState()
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
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            file = track.image
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
                                track.artists.let { artists ->
                                    when (artists) {
                                        is MediaController.MediaControllerState.Available.QueueItem.Artists.ImportArtist -> {
                                            artists.list.firstOrNull()?.let { artist ->
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Person, null)
                                                    Text(
                                                        text = artist,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        is MediaController.MediaControllerState.Available.QueueItem.Artists.SpotifyArtists -> {
                                            artists.list.firstOrNull()?.let { artist ->
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Person, null)
                                                    Text(
                                                        text = artist.second,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "${timePositionChange.formatted()}/${formattedDuration}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = {
                                    track.duration?.let {
                                        timePositionChange.inWholeMilliseconds.toFloat() / it.inWholeMilliseconds.toFloat()
                                    } ?: 0f
                                }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}