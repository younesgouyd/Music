package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import dev.younesgouyd.apps.music.common.components.util.AdaptiveUi
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.widgets.*
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumList(
    private val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val folderRepo: FolderRepo,
    private val mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Albums"
    private val state: MutableStateFlow<AlbumListState> = MutableStateFlow(AlbumListState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                AlbumListState.Loaded(
                    albums = albumRepo.getAll().mapLatest { list ->
                        list.map { dbAlbum ->
                            AlbumListState.Loaded.AlbumListItem(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date,
                                artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                    AlbumListState.Loaded.AlbumListItem.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    scrollState = LazyGridState(),
                    onAlbumClick = showAlbumDetails,
                    onArtistClick = showArtistDetails,
                    onPlayAlbumClick = ::playAlbum,
                    onAddAlbumToQueueClick = ::addAlbumToQueue,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
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

    private fun playAlbum(id: Long) {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun addAlbumToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun showAddToPlaylistDialog(albumId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Album(albumId),
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

    private sealed class AlbumListState {
        data object Loading : AlbumListState()

        data class Loaded(
            val albums: StateFlow<List<AlbumListItem>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val scrollState: LazyGridState,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onPlayAlbumClick: (Long) -> Unit,
            val onAddAlbumToQueueClick: (Long) -> Unit,
            val onAddToPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : AlbumListState() {
            data class AlbumListItem(
                val id: Long,
                val name: String,
                val image: ByteArray?,
                val releaseDate: String?,
                val artists: List<Artist>,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )
            }
        }
    }

    private object Ui {
        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: AlbumListState) {
                when (state) {
                    is AlbumListState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is AlbumListState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: AlbumListState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    albums = state.albums,
                    scrollState = state.scrollState,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onPlayAlbumClick = state.onPlayAlbumClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onAddAlbumToQueueClick = state.onAddAlbumToQueueClick
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
                albums: StateFlow<List<AlbumListState.Loaded.AlbumListItem>>,
                scrollState: LazyGridState,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onPlayAlbumClick: (Long) -> Unit,
                onAddToPlaylistClick: (id: Long) -> Unit,
                onAddAlbumToQueueClick: (Long) -> Unit
            ) {
                val items by albums.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                items(items = items, key = { it.id }) { album ->
                                    AlbumItem(
                                        album = album,
                                        onClick = { onAlbumClick(album.id) },
                                        onArtistClick = onArtistClick,
                                        onPlayClick = { onPlayAlbumClick(album.id) },
                                        onAddToPlaylistClick = { onAddToPlaylistClick(album.id) },
                                        onAddToQueueClick = { onAddAlbumToQueueClick(album.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )
            }

            @Composable
            private fun AlbumItem(
                modifier: Modifier = Modifier,
                album: AlbumListState.Loaded.AlbumListItem,
                onClick: () -> Unit,
                onArtistClick: (Long) -> Unit,
                onPlayClick: () -> Unit,
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            data = album.image,
                            contentScale = ContentScale.FillWidth,
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
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = album.releaseDate?.let { "Released: $it" } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = album.artists) { artist ->
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                content = { Icon(Icons.Default.PlayCircle, null) },
                                onClick = onPlayClick
                            )
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }

                        if (showContextMenu) {
                            ItemContextMenu(
                                item = Item(name = album.name, image = album.image),
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
                                    onClick = { onAddToQueueClick(); showContextMenu = false }
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

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: AlbumListState) {
                when (state) {
                    is AlbumListState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is AlbumListState.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: AlbumListState.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    albums = state.albums,
                    scrollState = state.scrollState,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onPlayAlbumClick = state.onPlayAlbumClick,
                    onAddToPlaylistClick = state.onAddToPlaylistClick,
                    onAddAlbumToQueueClick = state.onAddAlbumToQueueClick
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
                albums: StateFlow<List<AlbumListState.Loaded.AlbumListItem>>,
                scrollState: LazyGridState,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onPlayAlbumClick: (Long) -> Unit,
                onAddToPlaylistClick: (id: Long) -> Unit,
                onAddAlbumToQueueClick: (Long) -> Unit
            ) {
                val items by albums.collectAsState()

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
                                items(items = items, key = { it.id }) { album ->
                                    AlbumItem(
                                        album = album,
                                        onClick = { onAlbumClick(album.id) },
                                        onArtistClick = onArtistClick,
                                        onPlayClick = { onPlayAlbumClick(album.id) },
                                        onAddToPlaylistClick = { onAddToPlaylistClick(album.id) },
                                        onAddToQueueClick = { onAddAlbumToQueueClick(album.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )
            }

            @Composable
            private fun AlbumItem(
                modifier: Modifier = Modifier,
                album: AlbumListState.Loaded.AlbumListItem,
                onClick: () -> Unit,
                onArtistClick: (Long) -> Unit,
                onPlayClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { showContextMenu = true }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            data = album.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = album.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = album.artists) { artist ->
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
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(name = album.name, image = album.image),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = { onPlayClick(); showContextMenu = false },
                        )
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylistClick,
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = { onAddToQueueClick(); showContextMenu = false }
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