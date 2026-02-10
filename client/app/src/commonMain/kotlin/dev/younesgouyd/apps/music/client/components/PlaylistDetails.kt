package dev.younesgouyd.apps.music.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.Platform
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.platform
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import org.json.JSONArray
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetails(
    private val id: PlaylistId,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val artistRepo: SpotifyArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val folderRepo: FolderRepo,
    private val importSessionItemRepo: ImportSessionItemRepo,
    private val albumRepo: SpotifyAlbumRepo,
    private val mediaController: MediaController,
    mediaFileRepo: MediaFileRepo,
    showImport: (ImportSessionId) -> Unit,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Playlist"
    private val state: MutableStateFlow<PlaylistDetailsState> = MutableStateFlow(PlaylistDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")

    init {
        coroutineScope.launch {
            state.update {
                PlaylistDetailsState.Loaded(
                    playlist = playlistRepo.get(id).map { dbPlaylist ->
                        PlaylistDetailsState.Loaded.Playlist(
                            name = dbPlaylist.name,
                            image = dbPlaylist.importSessionId?.let { mediaFileRepo.getImportSessionImage(it) },
                            importUri = dbPlaylist.importUri,
                            onImportClick = { dbPlaylist.importSessionId?.let { showImport(it) } }
                        )
                    }.stateIn(coroutineScope),
                    tracks = searchQuery.flatMapLatest { nameQuery ->
                        trackRepo.searchPlaylist(this@PlaylistDetails.id, nameQuery).map {
                            it.map { dbTrack ->
                                PlaylistDetailsState.Loaded.Track(
                                    key = dbTrack.playlistTrackCrossRefId,
                                    id = dbTrack.track.id,
                                    name = dbTrack.spotifyTrack?.name ?: dbTrack.originalImport.title,
                                    album = dbTrack.spotifyTrack?.let { albumRepo.get(it.spotifyAlbumId).first().name },
                                    image = if (dbTrack.spotifyTrack != null) {
                                        mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack.spotifyAlbumId)
                                    } else {
                                        mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
                                    },
                                    artists = if (dbTrack.spotifyTrack != null) {
                                        artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack.id).first().map { dbArtist ->
                                            PlaylistDetailsState.Loaded.Track.Artist(dbArtist.id, dbArtist.name)
                                        }
                                    } else {
                                        val json = JSONArray(importSessionItemRepo.get(dbTrack.track.importSessionItemId).first().artists)
                                        buildList {
                                            for (i in 0 until json.length()) {
                                                add(
                                                    PlaylistDetailsState.Loaded.Track.Artist(
                                                        id = null,
                                                        name = json.getString(i)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }.stateIn(coroutineScope),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    searchQuery = searchQuery.asStateFlow(),
                    scrollState = LazyListState(),
                    onPlayClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(id))) },
                    onSearchQueryChange = { searchQuery.value = it },
                    changeItemPosition = { from, to ->
                        coroutineScope.launch {
                            playlistTrackCrossRefRepo.changeItemPosition(id, from, to)
                        }
                    },
                    onAddToQueueClick = { mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Playlist(id))) },
                    onAddToPlaylistClick = {
                        addToPlaylist.update {
                            AddToPlaylist(
                                itemToAdd = AddToPlaylist.Item.Playlist(id),
                                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                                trackRepo = trackRepo,
                                folderRepo = folderRepo,
                                artistRepo = artistRepo,
                                mediaFileRepo = mediaFileRepo,
                                dismiss = ::dismissAddToPlaylistDialog,
                                playlistRepo = playlistRepo,
                                albumRepo = albumRepo
                            )
                        }
                        addToPlaylistDialogVisible.update { true }
                    },
                    onTrackClick = { id ->
                        coroutineScope.launch {
                            val tracks = trackRepo.getPlaylistTracks(this@PlaylistDetails.id).first()
                            val index = tracks.indexOfFirst { it.track.id == id }
                            mediaController.playQueue(
                                queue = listOf(MediaController.QueueItemParameter.Playlist(this@PlaylistDetails.id)),
                                queueItemIndex = index
                            )
                        }
                    },
                    onArtistClick = showArtistDetails,
                    onAddTrackToPlaylistClick = { trackId: TrackId ->
                        addToPlaylist.update {
                            AddToPlaylist(
                                itemToAdd = AddToPlaylist.Item.Track(trackId),
                                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                                trackRepo = trackRepo,
                                folderRepo = folderRepo,
                                artistRepo = artistRepo,
                                mediaFileRepo = mediaFileRepo,
                                dismiss = ::dismissAddToPlaylistDialog,
                                playlistRepo = playlistRepo,
                                albumRepo = albumRepo
                            )
                        }
                        addToPlaylistDialogVisible.update { true }
                    },
                    onRemoveTrackFromPlaylistClick = { trackId: TrackId ->
                        coroutineScope.launch {
                            playlistTrackCrossRefRepo.delete(playlistId = id, trackId = trackId)
                        }
                    },
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onAddTrackToQueueClick = { id: TrackId ->
                        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(id)))
                    },
                    onShowTrackDetailsClick = showTrack
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

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    private sealed class PlaylistDetailsState {
        data object Loading : PlaylistDetailsState()

        data class Loaded(
            val playlist: StateFlow<Playlist>,
            val tracks: StateFlow<List<Track>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val searchQuery: StateFlow<String>,
            val scrollState: LazyListState,
            val onPlayClick: () -> Unit,
            val onSearchQueryChange: (String) -> Unit,
            val changeItemPosition: (from: Int, to: Int) -> Unit,
            val onAddToQueueClick: () -> Unit,
            val onAddToPlaylistClick: () -> Unit,
            val onTrackClick: (TrackId) -> Unit,
            val onArtistClick: (SpotifyArtistId) -> Unit,
            val onAddTrackToPlaylistClick: (TrackId) -> Unit,
            val onRemoveTrackFromPlaylistClick: (TrackId) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit,
            val onAddTrackToQueueClick: (TrackId) -> Unit,
            val onShowTrackDetailsClick: (TrackId) -> Unit
        ) : PlaylistDetailsState() {
            data class Playlist(
                val name: String,
                val image: File?,
                val importUri: String?,
                val onImportClick: () -> Unit
            )

            data class Track(
                val key: PlaylistTrackCrossRefId,
                val id: TrackId,
                val name: String,
                val album: String?,
                val image: File?,
                val artists: List<Artist>,
            ) {
                data class Artist(
                    val id: SpotifyArtistId?,
                    val name: String
                )
            }
        }
    }

    private object Ui {
        private const val KEY_PLAYLIST_INFO = "playlist_info"
        private const val KEY_TOOLBAR = "toolbar"
        private val itemHeight = 100.dp

        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: PlaylistDetailsState) {
                when (state) {
                    is PlaylistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is PlaylistDetailsState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: PlaylistDetailsState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    playlist = state.playlist,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    scrollState = state.scrollState,
                    onPlayClick = state.onPlayClick,
                    onSearchQueryChange = state.onSearchQueryChange,
                    changeItemPosition = state.changeItemPosition,
                    onAddToQueueClick = state.onAddToQueueClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onTrackClick = state.onTrackClick,
                    onArtistClick = state.onArtistClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onAddTrackToQueueClick = state.onAddTrackToQueueClick,
                    onRemoveTrackFromPlaylistClick = state.onRemoveTrackFromPlaylistClick,
                    onShowTrackDetailsClick = state.onShowTrackDetailsClick
                )

                if (addToPlaylistDialogVisible) {
                    Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                        addToPlaylist!!.show(Modifier)
                    }
                }
            }

            @OptIn(ExperimentalFoundationApi::class)
            @Composable
            private fun Main(
                modifier: Modifier,
                playlist: StateFlow<PlaylistDetailsState.Loaded.Playlist>,
                tracks: StateFlow<List<PlaylistDetailsState.Loaded.Track>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyListState,
                onPlayClick: () -> Unit,
                onSearchQueryChange: (String) -> Unit,
                changeItemPosition: (from: Int, to: Int) -> Unit,
                onAddToQueueClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onAddTrackToQueueClick: (TrackId) -> Unit,
                onRemoveTrackFromPlaylistClick: (TrackId) -> Unit,
                onShowTrackDetailsClick: (TrackId) -> Unit
            ) {
                val playlist by playlist.collectAsState()
                val items by tracks.collectAsState()
                var orderedItems by remember { mutableStateOf(items) }
                val searchQuery by searchQuery.collectAsState()
                var isDragging by remember { mutableStateOf(false) }
                val reorderState = rememberReorderableLazyListState(
                    onMove = { fromItem, toItem ->
                        isDragging = true
                        if (fromItem.index >= 2 && toItem.index >= 2) {
                            val some = orderedItems.toMutableList()
                            some.add(toItem.index - 2, some.removeAt(fromItem.index - 2))
                            orderedItems = some.toList()
                        }
                    },
                    listState = scrollState,
                    canDragOver = { from, to ->
                        from.key != KEY_PLAYLIST_INFO && from.key != KEY_TOOLBAR
                        && to.key != KEY_PLAYLIST_INFO && to.key != KEY_TOOLBAR
                    },
                    onDragEnd = { from, to ->
                        isDragging = false
                        if (from >= 2 && to >= 2) {
                            changeItemPosition(from - 2, to - 2)
                        }
                    }
                )

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = {
                        Box(modifier = Modifier.fillMaxSize().padding(it)) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                                    .padding(end = 16.dp)
                                    .reorderable(reorderState)
                                    .then(
                                        when (platform) {
                                            Platform.ANDROID -> Modifier.detectReorderAfterLongPress(reorderState)
                                            Platform.JVM -> Modifier.detectReorder(reorderState)
                                        }
                                    ),
                                state = scrollState,
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item(key = KEY_PLAYLIST_INFO) {
                                    ItemDetailsHeaderWide(
                                        modifier = Modifier.height(500.dp),
                                        title = playlist.name,
                                        image = playlist.image,
                                        itemAttributes = {
                                            if (playlist.importUri != null) {
                                                TextButton(
                                                    content = { Text("from: ${playlist.importUri}") },
                                                    onClick = playlist.onImportClick
                                                )
                                            }
                                        },
                                        mainAction = HeaderAction("Play", Icons.Default.PlayCircle, onPlayClick),
                                        actions = listOf(
                                            HeaderAction("Add to queue", Icons.Default.AddToQueue, onAddToQueueClick),
                                            HeaderAction("Add to playlist", Icons.AutoMirrored.Default.PlaylistAdd, onAddToPlaylistClick)
                                        )
                                    )
                                }
                                stickyHeader(key = KEY_TOOLBAR) {
                                    Surface(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                label = { Text("Search") },
                                                value = searchQuery,
                                                onValueChange = onSearchQueryChange
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            TracksHeader(modifier = Modifier.fillMaxWidth().height(64.dp))
                                            HorizontalDivider()
                                        }
                                    }
                                }
                                items(
                                    items = orderedItems,
                                    key = { item -> item.key.toString() },
                                ) { track ->
                                    ReorderableItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        state = reorderState,
                                        key = track.key.toString(),
                                        defaultDraggingModifier = Modifier.animateItem()
                                    ) { isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                        TrackItem(
                                            modifier = Modifier.fillMaxWidth().height(itemHeight),
                                            track = track,
                                            tonalElevation = elevation,
                                            onTrackClick = { onTrackClick(track.id) },
                                            onArtistClick = onArtistClick,
                                            onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                            onAddToQueueClick = { onAddTrackToQueueClick(track.id) },
                                            onRemoveFromPlaylistClick = { onRemoveTrackFromPlaylistClick(track.id) },
                                            onShowDetailsClick = { onShowTrackDetailsClick(track.id) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )

                LaunchedEffect(items) {
                    if (!isDragging) {
                        orderedItems = items
                    }
                }
            }

            private const val TITLE_WEIGHT = .45f
            private const val ALBUM_WEIGHT = .18f
            private const val ADDED_AT_WEIGHT = .1f
            private const val ACTIONS_WEIGHT = .1f

            @Composable
            private fun TracksHeader(modifier: Modifier) {
                Surface(
                    modifier = modifier
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Date added",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier,
                track: PlaylistDetailsState.Loaded.Track,
                tonalElevation: Dp,
                onTrackClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRemoveFromPlaylistClick: () -> Unit,
                onShowDetailsClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Surface(
                    modifier = modifier,
                    onClick = onTrackClick,
                    tonalElevation = tonalElevation
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // image + title + artists
                            Box(
                                modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                                        file = track.image
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxHeight().weight(1f),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(items = track.artists) { artist ->
                                                TextButton(
                                                    onClick = { onArtistClick(artist.id!!) },
                                                    enabled = artist.id != null,
                                                    content = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Person, null)
                                                            Text(
                                                                text = artist.name,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // album
                            Box(
                                modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (track.album != null) {
                                    Surface {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Album, null)
                                            Text(
                                                text = track.album,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // added at
                            Box(
                                modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "????-??-?? ??:??", // TODO
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // actions
                            Box(
                                modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    content = { Icon(Icons.Default.MoreVert, null) },
                                    onClick = { showContextMenu = true }
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = track.name,
                            image = track.image
                        ),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Details",
                            icon = Icons.Default.Info,
                            onClick = { onShowDetailsClick(); showContextMenu = false }
                        )
                        Option(
                            label = "Remove from playlist",
                            icon = Icons.Default.Remove,
                            onClick = { onRemoveFromPlaylistClick(); showContextMenu = false }
                        )
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylistClick
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

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: PlaylistDetailsState) {
                when (state) {
                    is PlaylistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is PlaylistDetailsState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: PlaylistDetailsState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    playlist = state.playlist,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    scrollState = state.scrollState,
                    onPlayClick = state.onPlayClick,
                    onSearchQueryChange = state.onSearchQueryChange,
                    changeItemPosition = state.changeItemPosition,
                    onAddToQueueClick = state.onAddToQueueClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onTrackClick = state.onTrackClick,
                    onArtistClick = state.onArtistClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onAddTrackToQueueClick = state.onAddTrackToQueueClick,
                    onRemoveTrackFromPlaylistClick = state.onRemoveTrackFromPlaylistClick,
                    onShowTrackDetailsClick = state.onShowTrackDetailsClick
                )

                if (addToPlaylistDialogVisible) {
                    Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                        addToPlaylist!!.show(Modifier)
                    }
                }
            }

            @OptIn(ExperimentalFoundationApi::class)
            @Composable
            private fun Main(
                modifier: Modifier,
                playlist: StateFlow<PlaylistDetailsState.Loaded.Playlist>,
                tracks: StateFlow<List<PlaylistDetailsState.Loaded.Track>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyListState,
                onPlayClick: () -> Unit,
                onSearchQueryChange: (String) -> Unit,
                changeItemPosition: (from: Int, to: Int) -> Unit,
                onAddToQueueClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onAddTrackToQueueClick: (TrackId) -> Unit,
                onRemoveTrackFromPlaylistClick: (TrackId) -> Unit,
                onShowTrackDetailsClick: (TrackId) -> Unit
            ) {
                val playlist by playlist.collectAsState()
                val items by tracks.collectAsState()
                var orderedItems by remember { mutableStateOf(items) }
                val searchQuery by searchQuery.collectAsState()
                var isDragging by remember { mutableStateOf(false) }
                val reorderState = rememberReorderableLazyListState(
                    onMove = { fromItem, toItem ->
                        isDragging = true
                        if (fromItem.index >= 2 && toItem.index >= 2) {
                            val some = orderedItems.toMutableList()
                            some.add(toItem.index - 2, some.removeAt(fromItem.index - 2))
                            orderedItems = some.toList()
                        }
                    },
                    listState = scrollState,
                    canDragOver = { from, to ->
                        from.key != KEY_PLAYLIST_INFO && from.key != KEY_TOOLBAR
                        && to.key != KEY_PLAYLIST_INFO && to.key != KEY_TOOLBAR
                    },
                    onDragEnd = { from, to ->
                        isDragging = false
                        if (from >= 2 && to >= 2) {
                            changeItemPosition(from - 2, to - 2)
                        }
                    }
                )

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = {
                        Box(modifier = Modifier.fillMaxSize().padding(it)) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                                    .reorderable(reorderState)
                                    .then(
                                        when (platform) {
                                            Platform.ANDROID -> Modifier.detectReorderAfterLongPress(reorderState)
                                            Platform.JVM -> Modifier.detectReorder(reorderState)
                                        }
                                    ),
                                state = scrollState,
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item(key = KEY_PLAYLIST_INFO) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ItemDetailsHeaderCompact(
                                            title = playlist.name,
                                            image = playlist.image,
                                            itemAttributes = {
                                                if (playlist.importUri != null) {
                                                    TextButton(
                                                        content = { Text("from: ${playlist.importUri}") },
                                                        onClick = playlist.onImportClick
                                                    )
                                                }
                                            },
                                            mainAction = HeaderAction("Play", Icons.Default.PlayCircle, onPlayClick),
                                            actions = listOf(
                                                HeaderAction("Add to queue", Icons.Default.AddToQueue, onAddToQueueClick),
                                                HeaderAction("Add to playlist", Icons.AutoMirrored.Default.PlaylistAdd, onAddToPlaylistClick)
                                            )
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                                stickyHeader(key = KEY_TOOLBAR) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Tracks",
                                                style = MaterialTheme.typography.titleLarge,
                                                textAlign = TextAlign.Start
                                            )
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                label = { Text("Search") },
                                                value = searchQuery,
                                                onValueChange = onSearchQueryChange
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }
                                items(
                                    items = orderedItems,
                                    key = { item -> item.key.toString() },
                                ) { track ->
                                    ReorderableItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        state = reorderState,
                                        key = track.key.toString(),
                                        defaultDraggingModifier = Modifier.animateItem()
                                    ) { isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                        TrackItem(
                                            modifier = Modifier.fillMaxWidth().height(itemHeight),
                                            track = track,
                                            tonalElevation = elevation,
                                            onClick = { onTrackClick(track.id) },
                                            onArtistClick = onArtistClick,
                                            onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                            onAddToQueueClick = { onAddTrackToQueueClick(track.id) },
                                            onRemoveFromPlaylistClick = { onRemoveTrackFromPlaylistClick(track.id) },
                                            onShowDetailsClick = { onShowTrackDetailsClick(track.id) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        ScrollToTopFloatingActionButton(
                            scrollState
                        )
                    }
                )

                LaunchedEffect(items) {
                    if (!isDragging) {
                        orderedItems = items
                    }
                }
            }

            private const val TITLE_WEIGHT = .45f
            private const val ACTIONS_WEIGHT = .1f

            @Composable
            private fun TrackItem(
                modifier: Modifier = Modifier,
                track: PlaylistDetailsState.Loaded.Track,
                tonalElevation: Dp,
                onClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRemoveFromPlaylistClick: () -> Unit,
                onShowDetailsClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Surface(
                    modifier = modifier,
                    onClick = onClick,
                    tonalElevation = tonalElevation
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                                        file = track.image
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxHeight().weight(1f),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (track.album != null) {
                                            Surface {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Album, null)
                                                    Text(
                                                        text = track.album,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(items = track.artists) { artist ->
                                                TextButton(
                                                    onClick = { onArtistClick(artist.id!!) },
                                                    enabled = artist.id != null,
                                                    content = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Person, null)
                                                            Text(
                                                                text = artist.name,
                                                                style = MaterialTheme.typography.labelMedium
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    content = { Icon(Icons.Default.MoreVert, null) },
                                    onClick = { showContextMenu = true }
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = track.name,
                            image = track.image
                        ),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Details",
                            icon = Icons.Default.Info,
                            onClick = { onShowDetailsClick(); showContextMenu = false }
                        )
                        Option(
                            label = "Remove from playlist",
                            icon = Icons.Default.Remove,
                            onClick = { onRemoveFromPlaylistClick(); showContextMenu = false }
                        )
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylistClick
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
}
