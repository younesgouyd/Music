package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import dev.younesgouyd.apps.music.common.Component
import dev.younesgouyd.apps.music.common.components.AlbumDetails.AlbumDetailsState.Loaded.Album
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.widgets.*
import dev.younesgouyd.apps.music.common.data.repoes.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetails(
    private val id: Long,
    private val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    private val trackRepo: TrackRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val playlistRepo: PlaylistRepo,
    private val folderRepo: FolderRepo,
    private val mediaController: MediaController,
    private val showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Album"
    private val state: MutableStateFlow<AlbumDetailsState> = MutableStateFlow(AlbumDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                AlbumDetailsState.Loaded(
                    album = albumRepo.get(id).mapLatest { dbAlbum ->
                        Album(
                            id = dbAlbum.id,
                            name = dbAlbum.name,
                            artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                Album.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name
                                )
                            },
                            image = dbAlbum.image,
                            releaseDate = dbAlbum.release_date
                        )
                    }.stateIn(coroutineScope),
                    tracks = trackRepo.getAlbumTracks(id).mapLatest { list ->
                        list.map { dbTrack ->
                            Album.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    Album.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onArtistClick = showArtistDetails,
                    onPlayClick = ::play,
                    onAddToQueueClick = ::addToQueue,
                    onTrackClick = ::playTrack,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onAddTrackToQueue = ::addTrackToQueue,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
                )
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun play() {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun addToQueue() {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun playTrack(id: Long) {
        coroutineScope.launch {
            val tracks = trackRepo.getAlbumTracksStatic(this@AlbumDetails.id)
            val index = tracks.indexOfFirst { it.id == id }
            mediaController.playQueue(
                queue = listOf(MediaController.QueueItemParameter.Album(this@AlbumDetails.id)),
                queueItemIndex = 0,
                queueSubItemIndex = index
            )
        }
    }

    private fun showAddToPlaylistDialog() {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Album(id),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                albumRepo = albumRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.update { true }
    }

    private fun addTrackToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(id)))
    }

    private fun showAddTrackToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Track(trackId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                albumRepo = albumRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.update { true }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    private sealed class AlbumDetailsState {
        data object Loading : AlbumDetailsState()

        data class Loaded(
            val album: StateFlow<Album>,
            val tracks: StateFlow<List<Album.Track>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onArtistClick: (id: Long) -> Unit,
            val onPlayClick: () -> Unit,
            val onAddToQueueClick: () -> Unit,
            val onTrackClick: (id: Long) -> Unit,
            val onAddToPlaylistClick: () -> Unit,
            val onAddTrackToPlaylistClick: (id: Long) -> Unit,
            val onAddTrackToQueue: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : AlbumDetailsState() {
            data class Album(
                val id: Long,
                val name: String,
                val artists: List<Artist>,
                val image: ByteArray?,
                val releaseDate: String?,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )

                data class Track(
                    val id: Long,
                    val name: String,
                    val artists: List<Artist>
                ) {
                    data class Artist(
                        val id: Long,
                        val name: String
                    )
                }
            }

        }
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: AlbumDetailsState) {
            when (state) {
                is AlbumDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AlbumDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: AlbumDetailsState.Loaded) {
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier = modifier,
                album = state.album,
                tracks = state.tracks,
                onArtistClick = state.onArtistClick,
                onPlayClick = state.onPlayClick,
                onAddToQueueClick = state.onAddToQueueClick,
                onAddToPlaylistClick = state.onAddToPlaylistClick,
                onTrackClick = state.onTrackClick,
                onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                onAddTrackToQueue = state.onAddTrackToQueue
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
            album: StateFlow<Album>,
            tracks: StateFlow<List<Album.Track>>,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onTrackClick: (Long) -> Unit,
            onAddTrackToPlaylistClick: (id: Long) -> Unit,
            onAddTrackToQueue: (id: Long) -> Unit
        ) {
            val album by album.collectAsState()
            val items by tracks.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        VerticalScrollbar(lazyColumnState)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyColumnState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                AlbumInfo(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    album = album,
                                    onArtistClick = onArtistClick,
                                    onPlayClick = onPlayClick,
                                    onAddToQueueClick = onAddToQueueClick,
                                    onAddToPlaylistClick = onAddToPlaylistClick
                                )
                            }
                            item {
                                Spacer(Modifier.size(8.dp))
                            }
                            stickyHeader {
                                TracksHeader(modifier = Modifier.fillMaxWidth().height(64.dp))
                                HorizontalDivider()
                            }
                            items(items = items, key = { it.id }) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    track = track,
                                    albumImage = album.image,
                                    onClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                    onAddToQueue = { onAddTrackToQueue(track.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyColumnState) }
            )
        }

        @Composable
        private fun AlbumInfo(
            modifier: Modifier,
            album: Album,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    data = album.image,
                    contentScale = ContentScale.FillHeight
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = album.name,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (artist in album.artists) {
                            TextButton(
                                onClick = { onArtistClick(artist.id) },
                                content = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        content = {
                                            Icon(Icons.Default.Person, null)
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                    if (album.releaseDate != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Release date: ${album.releaseDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayCircle, null)
                                    Text(text = "Play", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onPlayClick
                        )
                        OutlinedButton(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddToQueue, null)
                                    Text(text = "Add to queue", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onAddToQueueClick
                        )
                        OutlinedButton(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Default.PlaylistAdd, null)
                                    Text(text = "Add to playlist", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onAddToPlaylistClick
                        )
                    }
                }
            }
        }

        private const val TITLE_WEIGHT = .9f
        private const val ACTIONS_WEIGHT = .1f

        @Composable
        private fun TracksHeader(modifier: Modifier = Modifier) {
            Surface {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.Center) {
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
            modifier: Modifier = Modifier,
            track: Album.Track,
            albumImage: ByteArray?,
            onClick: () -> Unit,
            onArtistClick: (Long) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onAddToQueue: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }

            Row(
                modifier = modifier.clickable(onClick = onClick),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // image + title + artists
                Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.CenterStart) {
                    Column(
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
                            items(items = track.artists, key = { it.id }) { artist ->
                                TextButton(
                                    onClick = { onArtistClick(artist.id) },
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

                Spacer(Modifier.width(8.dp))

                // actions
                Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.CenterEnd) {
                    IconButton(
                        content = { Icon(Icons.Default.MoreVert, null) },
                        onClick = { showContextMenu = true }
                    )
                }
            }
            HorizontalDivider()

            if (showContextMenu) {
                ItemContextMenu(
                    item = Item(name = track.name, image = albumImage),
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
                        onClick = onAddToQueue,
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