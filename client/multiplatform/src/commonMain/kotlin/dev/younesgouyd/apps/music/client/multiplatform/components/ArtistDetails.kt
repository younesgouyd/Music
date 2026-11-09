package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.components.ArtistDetails.Ui.State.Loaded.Track
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ArtistDetails(
    id: SpotifyArtistId,
    artistRepo: SpotifyArtistRepo,
    albumRepo: SpotifyAlbumRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    playlistRepo: PlaylistRepo,
    mediaController: MediaController,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Artist"
    val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    private class Loader(
        private val artistId: SpotifyArtistId,
        private val query: String,
        private val trackRepo: TrackRepo
    ) : LazilyLoadedItems.Loader<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
        override suspend fun load(
            offset: Offset.Id<TrackId>,
            pageSize: PageSize
        ): LazilyLoadedItems.Page<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
            val rows = trackRepo.searchArtistContributions(artistId, query, pageSize, offset)
            return LazilyLoadedItems.Page(
                nextOffset = rows.lastOrNull()?.track?.id?.let {
                    Offset.Id(it)
                },
                items = rows.map { dbTrack ->
                    Ui.State.Loaded.Track(
                        id = dbTrack.track.id,
                        name = dbTrack.spotifyTrackRelation!!.spotifyAlbum.name,
                        image = dbTrack.spotifyTrackRelation!!.spotifyAlbum.mediumImgId ?: dbTrack.originalImport.imgId,
                        artists = dbTrack.spotifyTrackRelation!!.spotifyArtists.map { Pair(it.id, it.name) }
                    )
                }
            )
        }

    }

    init {
        coroutineScope.launch {
            val searchQuery = MutableStateFlow("")
            val selectedTab = MutableStateFlow(Pair(0, Ui.State.Loaded.Tab.entries.first()))
            val albums = searchQuery.flatMapLatest { search ->
                albumRepo.searchArtist(id, search).map { dbList ->
                    dbList.map { dbAlbum ->
                        Ui.State.Loaded.Album(
                            id = dbAlbum.id,
                            name = dbAlbum.name,
                            image = dbAlbum.mediumImgId,
                            artists = artistRepo.getSpotifyAlbumSpotifyArtists(dbAlbum.id).first().map {
                                Pair(it.id, it.name)
                            }
                        )
                    }
                }
            }.stateIn(coroutineScope)
            val tracks = LazilyLoadedItems<Ui.State.Loaded.Track, Offset.Id<TrackId>>(
                scope = coroutineScope,
                loader = searchQuery.debounce(2.seconds)
                    .mapLatest { Loader(id, it, trackRepo) }
                    .stateIn(coroutineScope),
                initialOffset = Offset.Id.initial<TrackId>()
            )
            artistRepo.get(id).collect { dbArtist ->
                if (dbArtist == null) {
                    state.value = Ui.State.ItemDoesNotExist
                    return@collect
                }
                state.value = Ui.State.Loaded(
                    scrollState = LazyGridState(),
                    selectedTab = selectedTab.asStateFlow(),
                    artist = Ui.State.Loaded.Artist(
                        id = dbArtist.id,
                        name = dbArtist.name,
                        image = dbArtist.mediumImgId
                    ),
                    albums = albums,
                    tracks = tracks,
                    searchQuery = searchQuery.asStateFlow(),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onTabClick = { selectedTab.value = it },
                    onPlayClick = { mediaController.playQueue(listOf(QueueItemParameter.Artist(id))) },
                    onSearchQueryChange = { searchQuery.value = it },
                    onAddToQueueClick = { mediaController.addToQueue(listOf(QueueItemParameter.Artist(id))) },
                    onAddToPlaylistClick = {
                        addToPlaylist.update {
                            AddToPlaylist(
                                itemToAdd = AddToPlaylist.Item.Artist(id),
                                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                                trackRepo = trackRepo,
                                folderRepo = folderRepo,
                                artistRepo = artistRepo,
                                dismiss = ::dismissAddToPlaylistDialog,
                                playlistRepo = playlistRepo,
                                albumRepo = albumRepo
                            )
                        }
                        addToPlaylistDialogVisible.update { true }
                    },
                    onArtistClick = showArtistDetails,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onAlbumClick = showAlbum,
                    onAddAlbumToPlaylistClick = { albumId ->
                        addToPlaylist.update {
                            it?.clear()
                            AddToPlaylist(
                                itemToAdd = AddToPlaylist.Item.Album(albumId),
                                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                                trackRepo = trackRepo,
                                folderRepo = folderRepo,
                                artistRepo = artistRepo,
                                albumRepo = albumRepo,
                                dismiss = ::dismissAddToPlaylistDialog,
                                playlistRepo = playlistRepo
                            )
                        }
                        addToPlaylistDialogVisible.value = true
                    },
                    onAddAlbumToQueueClick = { id -> mediaController.addToQueue(listOf(QueueItemParameter.Album(id))) },
                    onTrackClick = { mediaController.playQueue(listOf(QueueItemParameter.Track(it))) },
                    onShowTrackDetailsClick = showTrack,
                    onAddTrackToPlaylistClick = { trackId ->
                        addToPlaylist.update {
                            it?.clear()
                            AddToPlaylist(
                                itemToAdd = AddToPlaylist.Item.Track(trackId),
                                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                                trackRepo = trackRepo,
                                folderRepo = folderRepo,
                                artistRepo = artistRepo,
                                albumRepo = albumRepo,
                                dismiss = ::dismissAddToPlaylistDialog,
                                playlistRepo = playlistRepo
                            )
                        }
                        addToPlaylistDialogVisible.value = true
                    },
                    onAddTrackToQueueClick = { mediaController.addToQueue(listOf(QueueItemParameter.Track(it))) }
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            class Loaded(
                val scrollState: LazyGridState,
                val selectedTab: StateFlow<Pair<Int, Tab>>,
                val artist: Artist,
                val albums: StateFlow<List<Album>>,
                val tracks: LazilyLoadedItems<Track, Offset.Id<TrackId>>,
                val searchQuery: StateFlow<String>,
                val addToPlaylistDialogVisible: StateFlow<Boolean>,
                val addToPlaylist: StateFlow<Component?>,
                val onTabClick: (Pair<Int, State.Loaded.Tab>) -> Unit,
                val onPlayClick: () -> Unit,
                val onSearchQueryChange: (String) -> Unit,
                val onAddToQueueClick: () -> Unit,
                val onAddToPlaylistClick: () -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onDismissAddToPlaylistDialog: () -> Unit,
                val onAlbumClick: (SpotifyAlbumId) -> Unit,
                val onAddAlbumToPlaylistClick: (SpotifyAlbumId) -> Unit,
                val onAddAlbumToQueueClick: (SpotifyAlbumId) -> Unit,
                val onTrackClick: (TrackId) -> Unit,
                val onShowTrackDetailsClick: (TrackId) -> Unit,
                val onAddTrackToPlaylistClick: (TrackId) -> Unit,
                val onAddTrackToQueueClick: (TrackId) -> Unit
            ) : State() {
                enum class Tab { Discography, Contributions }

                data class Artist(
                    val id: SpotifyArtistId,
                    val name: String,
                    val image: MediaFileId?
                )

                data class Album(
                    val id: SpotifyAlbumId,
                    val name: String,
                    val image: MediaFileId?,
                    val artists: List<Pair<SpotifyArtistId, String>>
                )

                data class Track(
                    val id: TrackId,
                    val name: String,
                    val image: MediaFileId?,
                    val artists: List<Pair<SpotifyArtistId, String>>
                )
            }

            data object ItemDoesNotExist : State()
        }

        object Wide {
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
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    scrollState = state.scrollState,
                    selectedTab = state.selectedTab,
                    artist = state.artist,
                    albums = state.albums,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    onPlayClick = state.onPlayClick,
                    onTabClick = state.onTabClick,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onAddToQueueClick = state.onAddToQueueClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onAddAlbumToPlaylistClick = state.onAddAlbumToPlaylistClick,
                    onAddAlbumToQueueClick = state.onAddAlbumToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onShowTrackDetailsClick = state.onShowTrackDetailsClick,
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
                scrollState: LazyGridState,
                selectedTab: StateFlow<Pair<Int, State.Loaded.Tab>>,
                artist: State.Loaded.Artist,
                albums: StateFlow<List<State.Loaded.Album>>,
                tracks: LazilyLoadedItems<Track, Offset.Id<TrackId>>,
                searchQuery: StateFlow<String>,
                onTabClick: (Pair<Int, State.Loaded.Tab>) -> Unit,
                onPlayClick: () -> Unit,
                onSearchQueryChange: (String) -> Unit,
                onAddToQueueClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddAlbumToPlaylistClick: (SpotifyAlbumId) -> Unit,
                onAddAlbumToQueueClick: (SpotifyAlbumId) -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onShowTrackDetailsClick: (TrackId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onAddTrackToQueueClick: (TrackId) -> Unit
            ) {
                val albums by albums.collectAsState()
                val items by tracks.items.collectAsState()
                val loadingTracks by tracks.loading.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val selectedTab by selectedTab.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                state = scrollState,
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan)}) {
                                    ItemDetailsHeaderWide(
                                        modifier = Modifier.fillMaxWidth().height(500.dp),
                                        title = artist.name,
                                        image = artist.image,
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
                                            ),
                                            HeaderAction(
                                                "Add to playlist",
                                                Icons.AutoMirrored.Default.PlaylistAdd,
                                                onAddToPlaylistClick
                                            )
                                        )
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan)}) {
                                    PrimaryTabRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedTabIndex = selectedTab.first
                                    ) {
                                        State.Loaded.Tab.entries.forEachIndexed { index, item ->
                                            Tab(
                                                text = { Text(item.name) },
                                                selected = false,
                                                onClick = { onTabClick(Pair(index, item)) }
                                            )
                                        }
                                    }
                                }
                                item(span = { GridItemSpan(maxLineSpan)}) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        label = { Text("Search") },
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange
                                    )
                                }
                                when (selectedTab.second) {
                                    State.Loaded.Tab.Discography -> {
                                        items(
                                            items = albums,
                                            key = { it.id.value }
                                        ) { album ->
                                            AlbumItem(
                                                modifier = Modifier.fillMaxWidth(),
                                                album = album,
                                                onClick = { onAlbumClick(album.id) },
                                                onArtistClick = onArtistClick,
                                                onAddToPlaylistClick = { onAddAlbumToPlaylistClick(album.id) },
                                                onAddToQueueClick = { onAddAlbumToQueueClick(album.id) }
                                            )
                                        }
                                    }
                                    State.Loaded.Tab.Contributions -> {
                                        items(
                                            items = items,
                                            key = { it.id.value }
                                        ) { track ->
                                            TrackItem(
                                                modifier = Modifier.fillMaxWidth(),
                                                track = track,
                                                onClick = { onTrackClick(track.id) },
                                                onArtistClick = onArtistClick,
                                                onShowDetailsClick = { onShowTrackDetailsClick(track.id) },
                                                onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                                onAddToQueueClick = { onAddTrackToQueueClick(track.id) }
                                            )
                                        }
                                        if (loadingTracks) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )

                LaunchedEffect(scrollState, tracks) {
                    snapshotFlow {
                        scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    }.map { it == null ||  it >= (items.size + 3) - 5  }
                        .distinctUntilChanged()
                        .filter { it && selectedTab.second == State.Loaded.Tab.Contributions }
                        .collect { tracks.loadMore() }
                }
            }

            @Composable
            private fun AlbumItem(
                modifier: Modifier,
                album: State.Loaded.Album,
                onClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(1f),
                            id = album.image,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            text = album.name,
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
                            Artists(
                                modifier = Modifier.weight(1f),
                                artists = album.artists,
                                onArtistClick = onArtistClick
                            )
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = album.name,
                            image = album.image
                        ),
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier,
                track: State.Loaded.Track,
                onClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onShowDetailsClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(1f),
                            id = track.image,
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
                            Artists(
                                modifier = Modifier.weight(1f),
                                artists = track.artists,
                                onArtistClick = onArtistClick
                            )
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }
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
                            onClick = onShowDetailsClick,
                        )
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }
        }

        object Compact {
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
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    scrollState = state.scrollState,
                    selectedTab = state.selectedTab,
                    artist = state.artist,
                    albums = state.albums,
                    tracks = state.tracks,
                    searchQuery = state.searchQuery,
                    onTabClick = state.onTabClick,
                    onPlayClick = state.onPlayClick,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onAddToQueueClick = state.onAddToQueueClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onAlbumClick = state.onAlbumClick,
                    onAddAlbumToPlaylistClick = state.onAddAlbumToPlaylistClick,
                    onAddAlbumToQueueClick = state.onAddAlbumToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onShowTrackDetailsClick = state.onShowTrackDetailsClick,
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
                scrollState: LazyGridState,
                selectedTab: StateFlow<Pair<Int, State.Loaded.Tab>>,
                artist: State.Loaded.Artist,
                albums: StateFlow<List<State.Loaded.Album>>,
                tracks: LazilyLoadedItems<Track, Offset.Id<TrackId>>,
                searchQuery: StateFlow<String>,
                onTabClick: (Pair<Int, State.Loaded.Tab>) -> Unit,
                onPlayClick: () -> Unit,
                onSearchQueryChange: (String) -> Unit,
                onAddToQueueClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAlbumClick: (SpotifyAlbumId) -> Unit,
                onAddAlbumToPlaylistClick: (SpotifyAlbumId) -> Unit,
                onAddAlbumToQueueClick: (SpotifyAlbumId) -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onShowTrackDetailsClick: (TrackId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onAddTrackToQueueClick: (TrackId) -> Unit
            ) {
                val albums by albums.collectAsState()
                val items by tracks.items.collectAsState()
                val loadingTracks by tracks.loading.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val selectedTab by selectedTab.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                state = scrollState,
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(100.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    ItemDetailsHeaderCompact(
                                        title = artist.name,
                                        image = artist.image,
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
                                            ),
                                            HeaderAction(
                                                "Add to playlist",
                                                Icons.AutoMirrored.Default.PlaylistAdd,
                                                onAddToPlaylistClick
                                            )
                                        )
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan)}) {
                                    PrimaryTabRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedTabIndex = selectedTab.first
                                    ) {
                                        State.Loaded.Tab.entries.forEachIndexed { index, item ->
                                            Tab(
                                                text = { Text(item.name) },
                                                selected = false,
                                                onClick = { onTabClick(Pair(index, item)) }
                                            )
                                        }
                                    }
                                }
                                item(span = { GridItemSpan(maxLineSpan)}) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        label = { Text("Search") },
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange
                                    )
                                }
                                when (selectedTab.second) {
                                    State.Loaded.Tab.Discography -> {
                                        items(
                                            items = albums,
                                            key = { it.id.value }
                                        ) { album ->
                                            AlbumItem(
                                                modifier = Modifier.fillMaxWidth(),
                                                album = album,
                                                onClick = { onAlbumClick(album.id) },
                                                onAddToPlaylistClick = { onAddAlbumToPlaylistClick(album.id) },
                                                onAddToQueueClick = { onAddAlbumToQueueClick(album.id) }
                                            )
                                        }
                                    }
                                    State.Loaded.Tab.Contributions -> {
                                        items(
                                            items = items,
                                            key = { it.id.value }
                                        ) { track ->
                                            TrackItem(
                                                modifier = Modifier.fillMaxWidth(),
                                                track = track,
                                                onClick = { onTrackClick(track.id) },
                                                onShowDetailsClick = { onShowTrackDetailsClick(track.id) },
                                                onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                                onAddToQueueClick = { onAddTrackToQueueClick(track.id) }
                                            )
                                        }
                                        if (loadingTracks) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )

                LaunchedEffect(scrollState, tracks) {
                    snapshotFlow {
                        scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    }.map { it == null ||  it >= (items.size + 3) - 5  }
                        .distinctUntilChanged()
                        .filter { it && selectedTab.second == State.Loaded.Tab.Contributions }
                        .collect { tracks.loadMore() }
                }
            }

            @Composable
            private fun AlbumItem(
                modifier: Modifier,
                album: State.Loaded.Album,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(1f),
                            id = album.image,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            text = album.name,
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
                        item = Item(
                            name = album.name,
                            image = album.image
                        ),
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier,
                track: State.Loaded.Track,
                onClick: () -> Unit,
                onShowDetailsClick: () -> Unit,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(1f),
                            id = track.image,
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
                            onClick = onShowDetailsClick,
                        )
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }
        }
    }
}
