package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.MediaControllerState
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.Client
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.DeviceIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

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

        val isPlaying = mediaController.state.flatMapLatest { state ->
            when (state) {
                is MediaControllerState.Available -> state.mediaPlayerState.map { it?.isPlaying ?: false }
                is MediaControllerState.Loading,
                is MediaControllerState.Unavailable -> flowOf(false)
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaControllerState.Unavailable -> Ui.State.Unavailable
                is MediaControllerState.Loading -> Ui.State.Loading
                is MediaControllerState.Available -> Ui.State.Available(
                    clients = mediaControllerState.clients,
                    selectedClient = mediaControllerState.selectedClient,
                    enabled = mediaControllerState.enabled,
                    isPlaying = isPlaying,
                    onSelectClient = mediaController::selectClient,
                    track = mediaControllerState.currentItem,
                    isTrackVisible = isTrackVisible.asStateFlow(),
                    isQueueVisible = isQueueVisible.asStateFlow(),
                    repeatState = mediaControllerState.repeatState,
                    positionMs = mediaControllerState.positionMs,
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
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Unavailable)
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
                val clients: StateFlow<List<Client>>,
                val selectedClient: StateFlow<ClientId>,
                val enabled: StateFlow<Boolean>,
                val isPlaying: StateFlow<Boolean>,
                val onSelectClient: (ClientId) -> Unit,
                val track: StateFlow<QueueItem?>,
                val isTrackVisible: StateFlow<Boolean>,
                val isQueueVisible: StateFlow<Boolean>,
                val repeatState: StateFlow<RepeatState>,
                val positionMs: StateFlow<Long?>,
                val queue: @Composable (Modifier) -> Unit,
                val onToggleQueueVisibility: () -> Unit,
                val onToggleTrackVisibility: () -> Unit,
                val onTrackNameClick: (TrackId) -> Unit,
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

        private object Common {
            @Composable
            fun TrackInfo(
                modifier: Modifier,
                track: QueueItem?,
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
                    track?.album.let { album ->
                        when (album) {
                            is QueueItem.Album.ImportAlbum -> {
                                Album(
                                    name = album.name ?: ""
                                )
                            }
                            is QueueItem.Album.SpotifyAlbum -> {
                                Album(
                                    name = album.name,
                                    onClick = { onAlbumClick(album.id) })
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
                                Artists(
                                    artists = artists.list,
                                    onArtistClick = onArtistClick
                                )
                            }
                            null -> {
                                Artists(names = emptyList())
                            }
                        }
                    }
                }
            }

            @Composable
            fun PlaybackControls(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                track: QueueItem?,
                isPlaying: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                positionMs: StateFlow<Long?>,
                onTimeChange: (positionMs: Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val isPlaying by isPlaying.collectAsState()
                val repeatState by repeatState.collectAsState()
                val formattedDuration = remember(track?.durationMs) { track?.durationMs?.milliseconds.formatted() }
                val positionMs by positionMs.collectAsState()

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlaybackSlider(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = enabled,
                        durationMs = track?.durationMs,
                        positionMs = positionMs,
                        onSeek = onTimeChange
                    )
                    Text(
                        text = "${positionMs?.milliseconds.formatted()}/${formattedDuration}",
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
                            onClick = onPreviousClick
                        ) {
                            Icon(Icons.Default.SkipPrevious, null)
                        }
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
                            onClick = onNextClick
                        ) {
                            Icon(Icons.Default.SkipNext, null)
                        }
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

            @Composable
            fun Clients(
                modifier: Modifier,
                enabled: StateFlow<Boolean>,
                clients: StateFlow<List<Client>>,
                selectedClient: StateFlow<ClientId>,
                onSelectClient: (ClientId) -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val clients by clients.collectAsState()
                val selectedClient by selectedClient.collectAsState()

                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(
                            items = clients,
                            key = { it.id.value }
                        ) { client ->
                            val isSelected = client.id == selectedClient
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                enabled = enabled,
                                onClick = { onSelectClient(client.id) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.RadioButtonChecked, null)
                                        } else {
                                            Icon(Icons.Default.RadioButtonUnchecked, null)
                                        }
                                    }
                                    DeviceIcon(client.deviceType)
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "${client.id} - ${client.name}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
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
                    clients = state.clients,
                    selectedClient = state.selectedClient,
                    enabled = state.enabled,
                    track = state.track,
                    isQueueVisible = state.isQueueVisible,
                    isTrackVisible = state.isTrackVisible,
                    repeatState = state.repeatState,
                    positionMs = state.positionMs,
                    queue = state.queue,
                    onToggleTrackVisibility = state.onToggleTrackVisibility,
                    onToggleQueueVisibility = state.onToggleQueueVisibility,
                    isPlaying = state.isPlaying,
                    onSelectClient = state.onSelectClient,
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
                clients: StateFlow<List<Client>>,
                selectedClient: StateFlow<ClientId>,
                enabled: StateFlow<Boolean>,
                track: StateFlow<QueueItem?>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                positionMs: StateFlow<Long?>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                isPlaying: StateFlow<Boolean>,
                onSelectClient: (ClientId) -> Unit,
                onTrackNameClick: (TrackId) -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onTimeChange: (positionMs: Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val isQueueVisible by isQueueVisible.collectAsState()
                val isTrackVisible by isTrackVisible.collectAsState()
                var isClientsVisible by remember { mutableStateOf(false) }

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
                            IconButton(
                                onClick = { isClientsVisible = true },
                                content = { Icon(Icons.Default.Devices, null) }
                            )
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
                            positionMs = positionMs,
                            onTimeChange = onTimeChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick,
                            onRepeatClick = onRepeatClick
                        )
                    }
                }

                if (isClientsVisible) {
                    Dialog(onDismissRequest = { isClientsVisible = false }) {
                        Common.Clients(
                            modifier = modifier.size(width = 500.dp, height = 600.dp),
                            enabled = enabled,
                            clients = clients,
                            selectedClient = selectedClient,
                            onSelectClient = onSelectClient,
                        )
                    }
                }
            }

            @Composable
            private fun Track(
                modifier: Modifier,
                track: QueueItem?,
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
                                    id = track?.imageId
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
                                    id = track?.imageId
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
                    clients = state.clients,
                    selectedClient = state.selectedClient,
                    enabled = state.enabled,
                    track = state.track,
                    isQueueVisible = state.isQueueVisible,
                    isTrackVisible = state.isTrackVisible,
                    repeatState = state.repeatState,
                    positionMs = state.positionMs,
                    queue = state.queue,
                    onToggleTrackVisibility = state.onToggleTrackVisibility,
                    onToggleQueueVisibility = state.onToggleQueueVisibility,
                    isPlaying = state.isPlaying,
                    onSelectClient = state.onSelectClient,
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
                clients: StateFlow<List<Client>>,
                selectedClient: StateFlow<ClientId>,
                enabled: StateFlow<Boolean>,
                track: StateFlow<QueueItem?>,
                isQueueVisible: StateFlow<Boolean>,
                isTrackVisible: StateFlow<Boolean>,
                repeatState: StateFlow<RepeatState>,
                positionMs: StateFlow<Long?>,
                queue: @Composable (Modifier) -> Unit,
                onToggleTrackVisibility: () -> Unit,
                onToggleQueueVisibility: () -> Unit,
                isPlaying: StateFlow<Boolean>,
                onSelectClient: (ClientId) -> Unit,
                onTrackNameClick: (TrackId) -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onTimeChange: (positionMs: Long) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: () -> Unit
            ) {
                val track by track.collectAsState()
                val isQueueVisible by isQueueVisible.collectAsState()
                val isTrackVisible by isTrackVisible.collectAsState()
                var isClientsVisible by remember { mutableStateOf(false) }

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
                            IconButton(
                                onClick = { isClientsVisible = true },
                                content = { Icon(Icons.Default.Devices, null) }
                            )
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
                            positionMs = positionMs,
                            onTimeChange = onTimeChange,
                            onPreviousClick = onPreviousClick,
                            onPlayClick = onPlayClick,
                            onPauseClick = onPauseClick,
                            onNextClick = onNextClick,
                            onRepeatClick = onRepeatClick
                        )
                    }
                }

                if (isClientsVisible) {
                    Dialog(onDismissRequest = { isClientsVisible = false }) {
                        Common.Clients(
                            modifier = modifier.size(width = 500.dp, height = 600.dp),
                            enabled = enabled,
                            clients = clients,
                            selectedClient = selectedClient,
                            onSelectClient = onSelectClient,
                        )
                    }
                }
            }

            @Composable
            private fun Track(
                modifier: Modifier,
                track: QueueItem?,
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
                        id = track?.imageId
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