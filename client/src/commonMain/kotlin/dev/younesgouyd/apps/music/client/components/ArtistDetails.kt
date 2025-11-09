package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetails(
    private val id: Long,
    private val artistRepo: ArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val mediaController: MediaController,
    private val showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Artist"
    private val state: MutableStateFlow<ArtistDetailsState> = MutableStateFlow(ArtistDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")

    init {
        coroutineScope.launch {
            state.update {
                ArtistDetailsState.Loaded(
                    artist = artistRepo.get(id).mapLatest { dbArtist ->
                        ArtistDetailsState.Loaded.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name,
                            image = dbArtist.image
                        )
                    }.stateIn(coroutineScope),
                    tracks = searchQuery.flatMapLatest { nameQuery ->
                        trackRepo.searchArtist(this@ArtistDetails.id, nameQuery).mapLatest { dbList ->
                            dbList.map { dbTrack ->
                                ArtistDetailsState.Loaded.Track(
                                    id = dbTrack.id,
                                    name = dbTrack.name,
                                    image = dbTrack.albumArt?.let { Base64.decode(it) },
                                    artists = artistRepo.getTrackArtists(dbTrack.id).mapLatest {
                                        it.map { dbArtist ->
                                            ArtistDetailsState.Loaded.Track.Artist(
                                                id = dbArtist.id,
                                                name = dbArtist.name
                                            )
                                        }
                                    }.first()
                                )
                            }
                        }
                    }.stateIn(coroutineScope),
                    searchQuery = searchQuery.asStateFlow(),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    scrollState = LazyGridState(),
                    onSearchQueryChange = { searchQuery.value = it },
                    onArtistClick = showArtistDetails,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onTrackClick = ::playTrack,
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onAddTrackToQueueClick = ::addTrackToQueueClick
                )
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

    private fun playTrack(id: Long) {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(id)))
    }

    private fun showAddTrackToPlaylistDialog(albumId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Track(albumId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.update { true }
    }

    private fun addTrackToQueueClick(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(id)))
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    private sealed class ArtistDetailsState {
        data object Loading : ArtistDetailsState()

        data class Loaded(
            val artist: StateFlow<Artist>,
            val tracks: StateFlow<List<Track>>,
            val searchQuery: StateFlow<String>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val scrollState: LazyGridState,
            val onSearchQueryChange: (String) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit,
            val onTrackClick: (Long) -> Unit,
            val onAddTrackToPlaylistClick: (Long) -> Unit,
            val onAddTrackToQueueClick: (Long) -> Unit
        ) : ArtistDetailsState() {
            data class Artist(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )

            data class Track(
                val id: Long,
                val name: String,
                val image: ByteArray?,
                val artists: List<Artist>
            ) {
                data class Artist(
                    val id: Long,
                    val name: String,
                )
            }
        }
    }

    private object Ui {
        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: ArtistDetailsState) {
                when (state) {
                    is ArtistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is ArtistDetailsState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: ArtistDetailsState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    artist = state.artist,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    scrollState = state.scrollState,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onTrackClick = state.onTrackClick,
                    onArtistClick = state.onArtistClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onAddTrackToQueueClick = state.onAddTrackToQueueClick
                )

                if (addToPlaylistDialogVisible) {
                    Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                        addToPlaylist!!.show(Modifier)
                    }
                }
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artist: StateFlow<ArtistDetailsState.Loaded.Artist>,
                tracks: StateFlow<List<ArtistDetailsState.Loaded.Track>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyGridState,
                onSearchQueryChange: (String) -> Unit,
                onTrackClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onAddTrackToPlaylistClick: (Long) -> Unit,
                onAddTrackToQueueClick: (Long) -> Unit
            ) {
                val artist by artist.collectAsState()
                val trackItems by tracks.collectAsState()
                val searchQuery by searchQuery.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                                state = scrollState,
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    ArtistInfo(
                                        modifier = Modifier.fillMaxWidth().height(400.dp),
                                        artist = artist,
                                    )
                                }
                                stickyHeader {
                                    Surface {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Tracks",
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                label = { Text("Search") },
                                                value = searchQuery,
                                                onValueChange = onSearchQueryChange
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = trackItems,
                                    key = { it.id }
                                ) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onAddToQueueClick = { onAddTrackToQueueClick(track.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )
            }

            @Composable
            private fun ArtistInfo(
                modifier: Modifier = Modifier,
                artist: ArtistDetailsState.Loaded.Artist
            ) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.fillMaxHeight(),
                        data = artist.image,
                        contentScale = ContentScale.FillHeight
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = artist.name,
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier = Modifier,
                track: ArtistDetailsState.Loaded.Track,
                onClick: () -> Unit,
                onArtistClick: (Long) -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            data = track.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(items = track.artists) { artist ->
                                    TextButton(
                                        onClick = { onArtistClick(artist.id) },
                                        content = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Person, null)
                                                Text(artist.name)
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(name = track.name, image = track.image),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylistClick,
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueueClick,
                        )
                        Option(
                            label = "Play next",
                            icon = Icons.Default.QueuePlayNext,
                            onClick = { TODO() },
                        )
                    }
                }
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: ArtistDetailsState) {
                when (state) {
                    is ArtistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is ArtistDetailsState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: ArtistDetailsState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    artist = state.artist,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    scrollState = state.scrollState,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onTrackClick = state.onTrackClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onAddTrackToQueueClick = state.onAddTrackToQueueClick
                )

                if (addToPlaylistDialogVisible) {
                    Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                        addToPlaylist!!.show(Modifier)
                    }
                }
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artist: StateFlow<ArtistDetailsState.Loaded.Artist>,
                tracks: StateFlow<List<ArtistDetailsState.Loaded.Track>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyGridState,
                onSearchQueryChange: (String) -> Unit,
                onTrackClick: (Long) -> Unit,
                onAddTrackToPlaylistClick: (id: Long) -> Unit,
                onAddTrackToQueueClick: (id: Long) -> Unit
            ) {
                val artist by artist.collectAsState()
                val trackItems by tracks.collectAsState()
                val searchQuery by searchQuery.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(100.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    ArtistInfo(
                                        modifier = Modifier.fillMaxWidth(),
                                        artist = artist,
                                    )
                                }
                                stickyHeader {
                                    Surface {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Tracks",
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                label = { Text("Search") },
                                                value = searchQuery,
                                                onValueChange = onSearchQueryChange
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = trackItems,
                                    key = { it.id }
                                ) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onAddToQueueClick = { onAddTrackToQueueClick(track.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )
            }

            @Composable
            private fun ArtistInfo(
                modifier: Modifier,
                artist: ArtistDetailsState.Loaded.Artist
            ) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        data = artist.image,
                        contentScale = ContentScale.FillWidth
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = artist.name,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier = Modifier,
                track: ArtistDetailsState.Loaded.Track,
                onClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            data = track.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(name = track.name, image = track.image),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylistClick,
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueueClick,
                        )
                        Option(
                            label = "Play next",
                            icon = Icons.Default.QueuePlayNext,
                            onClick = { TODO() },
                        )
                    }
                }
            }
        }
    }
}
