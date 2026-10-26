package dev.younesgouyd.apps.music.client.multiplatform.components

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
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.MediaControllerState
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.common.models.RepeatState
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem
import dev.younesgouyd.apps.music.common.ui.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class MiniPlayer(
    mediaController: MediaController,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit,
    expand: () -> Unit
) : Component() {
    override val title: String = "Mini Player"
    private val state: StateFlow<Ui.State>

    init {
        state = mediaController.state.map { mediaControllerState ->
            when (mediaControllerState) {
                is MediaControllerState.Unavailable -> Ui.State.Unavailable
                is MediaControllerState.Loading -> Ui.State.Loading
                is MediaControllerState.Available -> Ui.State.Available(
                    enabled = mediaControllerState.enabled,
                    repeatState = mediaControllerState.repeatState,
                    track = mediaControllerState.currentItem,
                    positionMs = mediaControllerState.positionMs,
                    isPlaying = mediaControllerState.mediaPlayerState.map { it?.isPlaying ?: false }
                        .stateIn(coroutineScope),
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
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Unavailable)
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            data object Unavailable : State()

            data class Available(
                val enabled: StateFlow<Boolean>,
                val repeatState: StateFlow<RepeatState>,
                val track: StateFlow<QueueItem?>,
                val positionMs: StateFlow<Long?>,
                val isPlaying: StateFlow<Boolean>,
                val onClick: () -> Unit,
                val onAlbumClick: (SpotifyAlbumId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onTimeChange: (positionMs: Long) -> Unit,
                val onPreviousClick: () -> Unit,
                val onPlayClick: () -> Unit,
                val onPauseClick: () -> Unit,
                val onNextClick: () -> Unit,
                val onRepeatClick: () -> Unit
            ) : State()
        }

        object Wide {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: State) {
                when (state) {
                    is State.Loading -> Unit
                    is State.Unavailable -> Unit
                    is State.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: State.Available) {
                Main(
                    modifier = modifier,
                    enabled = state.enabled,
                    isPlaying = state.isPlaying,
                    repeatState = state.repeatState,
                    positionMs = state.positionMs,
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
                track: StateFlow<QueueItem?>,
                isPlaying: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                positionMs: StateFlow<Long?>,
                onClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onTimeChange: (positionMs: Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val track by track.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val repeatState by repeatState.collectAsState()
                val formattedDuration = remember(track?.durationMs) { track?.durationMs?.milliseconds.formatted() }
                val positionMs by positionMs.collectAsState()

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
                            id = track?.imageId
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = track?.name ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            track?.album.let { album ->
                                when (album) {
                                    is QueueItem.Album.ImportAlbum -> {
                                        Album(name = album.name ?: "")
                                    }
                                    is QueueItem.Album.SpotifyAlbum -> {
                                        Album(
                                            name = album.name,
                                            onClick = { onAlbumClick(album.id) }
                                        )
                                    }
                                    null -> {
                                        Album(name = "")
                                    }
                                }
                            }
                            track?.artists.let { artists ->
                                when (artists) {
                                    is QueueItem.Artists.ImportArtist -> {
                                        Artists(names = artists.list)
                                    }
                                    is QueueItem.Artists.SpotifyArtists -> {
                                        Artists(artists = artists.list, onArtistClick = onArtistClick)
                                    }
                                    null -> {
                                        Artists(names = emptyList())
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
                                                    RepeatState.Off -> Icon(
                                                        Icons.Default.Repeat,
                                                        null
                                                    )

                                                    RepeatState.Track -> Icon(
                                                        Icons.Default.RepeatOneOn,
                                                        null
                                                    )

                                                    RepeatState.List -> Icon(
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
                                    durationMs = track?.durationMs,
                                    positionMs = positionMs,
                                    onSeek = onTimeChange
                                )
                                Text(
                                    text = "${positionMs?.milliseconds.formatted()}/${formattedDuration}",
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
            fun Main(modifier: Modifier = Modifier, state: State) {
                when (state) {
                    is State.Loading -> Unit
                    is State.Unavailable -> Unit
                    is State.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: State.Available) {
                Main(
                    modifier = modifier,
                    track = state.track,
                    positionMs = state.positionMs,
                    onClick = state.onClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                track: StateFlow<QueueItem?>,
                positionMs: StateFlow<Long?>,
                onClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val formattedDuration = remember(track?.durationMs) { track?.durationMs?.milliseconds.formatted() }
                val positionMs by positionMs.collectAsState()

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
                            id = track?.imageId
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = track?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                track?.artists.let { artists ->
                                    when (artists) {
                                        is QueueItem.Artists.ImportArtist -> {
                                            Artists(names = artists.list)
                                        }
                                        is QueueItem.Artists.SpotifyArtists -> {
                                            Artists(names = artists.list.map { it.second })
                                        }
                                        null -> {
                                            Artists(names = emptyList())
                                        }
                                    }
                                }
                                Text(
                                    text = "${positionMs?.milliseconds.formatted()}/${formattedDuration}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = {
                                    track?.durationMs?.let {
                                        (positionMs?.toFloat() ?: 0f) / it.toFloat()
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