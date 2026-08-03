package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.MediaController
import dev.younesgouyd.apps.music.client.common.components.util.*
import dev.younesgouyd.apps.music.client.common.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.SpotifyAlbumRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.SpotifyTrackRepo
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetails(
    id: SpotifyAlbumId,
    albumRepo: SpotifyAlbumRepo,
    mediaFileRepo: MediaFileRepo,
    artistRepo: SpotifyArtistRepo,
    spotifyTrackRepo: SpotifyTrackRepo,
    mediaController: MediaController,
    showTrack: (TrackId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Album"
    private val state: StateFlow<Ui.State>

    init {
        val album = albumRepo.get(id).filterNotNull().map {
            Ui.State.Loaded.Album(
                name = it.name,
                image = mediaFileRepo.getSpotifyAlbumImage(id),
                artists = artistRepo.getSpotifyAlbumSpotifyArtists(id).first().map { dbArtist ->
                    Pair(dbArtist.id, dbArtist.name)
                }
            )
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        val spotifyTracks = spotifyTrackRepo.getAlbumTracks(id).mapLatest { dbList ->
            dbList.map { dbTrack ->
                Ui.State.Loaded.Track(
                    id = dbTrack.spotifyTrack.id,
                    trackId = dbTrack.track?.id,
                    name = dbTrack.spotifyTrack.name,
                    artists = artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack.id).first().map { dbArtist ->
                        Pair(dbArtist.id, dbArtist.name)
                    }
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
        var loaded: Ui.State.Loaded? = null
        state = album.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    scrollState = LazyListState(),
                    album = album.filterNotNull().stateIn(coroutineScope),
                    tracks = spotifyTracks,
                    onPlayClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id))) },
                    onAddToQueueClick = {
                        mediaController.addToQueue(
                            items = spotifyTracks.value.mapNotNull {
                                it.trackId?.let {
                                    MediaController.QueueItemParameter.Track(it)
                                }
                            }
                        )
                    },
                    onTrackClick = { trackId ->
                        val tracks = spotifyTracks.value.mapNotNull { it.trackId }
                        mediaController.playQueue(
                            queue = tracks.map { MediaController.QueueItemParameter.Track(it) },
                            queueItemIndex = tracks.indexOfFirst { it == trackId }
                        )
                    },
                    onArtistClick = showArtist,
                    onAddTrackToQueueClick = {
                        mediaController.addToQueue(
                            listOf(MediaController.QueueItemParameter.Track(it))
                        )
                    },
                    onShowTrackDetailsClick = showTrack
                )
            }
        }.map {
            if (it == null) {
                Ui.State.ItemDoesNotExist
            } else {
                loaded!!
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Loading)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }


    private object Ui {
        private const val KEY_ALBUM_INFO = "album_info"
        private val itemHeight = 100.dp

        sealed class State {
            data object Loading : State()

            data class Loaded(
                val scrollState: LazyListState,
                val album: StateFlow<Album>,
                val tracks: StateFlow<List<Track>>,
                val onPlayClick: () -> Unit,
                val onAddToQueueClick: () -> Unit,
                val onTrackClick: (TrackId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onAddTrackToQueueClick: (TrackId) -> Unit,
                val onShowTrackDetailsClick: (TrackId) -> Unit
            ) : State() {
                data class Album(
                    val name: String,
                    val image: File?,
                    val artists: List<Pair<SpotifyArtistId, String>>
                )

                data class Track(
                    val id: SpotifyTrackId,
                    val trackId: TrackId?,
                    val name: String,
                    val artists: List<Pair<SpotifyArtistId, String>>
                )
            }

            data object ItemDoesNotExist : State()
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, state = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: State.Loaded) {
            Main(
                modifier = modifier,
                scrollState = state.scrollState,
                album = state.album,
                tracks = state.tracks,
                onPlayClick = state.onPlayClick,
                onAddToQueueClick = state.onAddToQueueClick,
                onTrackClick = state.onTrackClick,
                onArtistClick = state.onArtistClick,
                onAddTrackToQueueClick = state.onAddTrackToQueueClick,
                onShowTrackDetailsClick = state.onShowTrackDetailsClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            scrollState: LazyListState,
            album: StateFlow<State.Loaded.Album>,
            tracks: StateFlow<List<State.Loaded.Track>>,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onAddTrackToQueueClick: (TrackId) -> Unit,
            onShowTrackDetailsClick: (TrackId) -> Unit
        ) {
            val album by album.collectAsState()
            val tracks by tracks.collectAsState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = {
                    Box(modifier = Modifier.fillMaxSize().padding(it)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = scrollState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item(key = KEY_ALBUM_INFO) {
                                AdaptiveUi(
                                    wide = {
                                        ItemDetailsHeaderWide(
                                            modifier = Modifier.height(500.dp),
                                            title = album.name,
                                            image = album.image,
                                            itemAttributes = {
                                                Artists(
                                                    artists = album.artists,
                                                    onArtistClick = onArtistClick
                                                )
                                            },
                                            mainAction = HeaderAction(
                                                "Play",
                                                Icons.Default.PlayCircle,
                                                onPlayClick
                                            ),
                                            actions = listOf(
                                                HeaderAction(
                                                    "Add to queue",
                                                    Icons.Default.AddToQueue,
                                                    onAddToQueueClick
                                                )
                                            )
                                        )
                                    },
                                    compact = {
                                        ItemDetailsHeaderCompact(
                                            title = album.name,
                                            image = album.image,
                                            itemAttributes = {
                                                Artists(
                                                    artists = album.artists,
                                                    onArtistClick = onArtistClick
                                                )
                                            },
                                            mainAction = HeaderAction(
                                                "Play",
                                                Icons.Default.PlayCircle,
                                                onPlayClick
                                            ),
                                            actions = listOf(
                                                HeaderAction(
                                                    "Add to queue",
                                                    Icons.Default.AddToQueue,
                                                    onAddToQueueClick
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                            items(
                                items = tracks,
                                key = { item -> item.id.value.toString() },
                            ) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                                    track = track,
                                    albumImage = album.image,
                                    enabled = track.trackId != null,
                                    onTrackClick = { onTrackClick(track.trackId!!) },
                                    onArtistClick = onArtistClick,
                                    onAddToQueueClick = { onAddTrackToQueueClick(track.trackId!!) },
                                    onDetailsClick = { onShowTrackDetailsClick(track.trackId!!) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
            )
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier,
            track: State.Loaded.Track,
            albumImage: File?,
            enabled: Boolean,
            onTrackClick: () -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onAddToQueueClick: () -> Unit,
            onDetailsClick: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                onClick = onTrackClick,
                enabled = enabled
            ) {
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Artists(
                            artists = track.artists,
                            onArtistClick = onArtistClick
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        content = { Icon(Icons.Default.MoreVert, null) },
                        onClick = { showContextMenu = true },
                        enabled = enabled
                    )
                }
            }

            if (showContextMenu && enabled) {
                ItemContextMenu(
                    item = Item(
                        name = track.name,
                        image = albumImage
                    ),
                    onDismiss = { showContextMenu = false }
                ) {
                    Option(
                        label = "Details",
                        icon = Icons.Default.Info,
                        onClick = { onDetailsClick(); showContextMenu = false }
                    )
                    Option(
                        label = "Add to queue",
                        icon = Icons.Default.AddToQueue,
                        onClick = { onAddToQueueClick(); showContextMenu = false }
                    )
                    Option(
                        label = "Play next",
                        icon = Icons.Default.QueuePlayNext,
                        onClick = { TODO() }
                    )
                }
            }
        }
    }
}