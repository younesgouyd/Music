package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.Item
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistList(
    private val playlistRepo: PlaylistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val albumRepo: AlbumRepo,
    showPlaylistDetails: (Long) -> Unit,
    playPlaylist: (Long) -> Unit
) : Component() {
    override val title: String = "Playlists"
    private val state: MutableStateFlow<PlaylistListState> = MutableStateFlow(PlaylistListState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                PlaylistListState.Loaded(
                    playlists = playlistRepo.getAll().mapLatest { list ->
                        list.map { dbPlaylist ->
                            PlaylistListState.Loaded.PlaylistListItem(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = dbPlaylist.image
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onPlaylistClick = showPlaylistDetails,
                    onPlayPlaylistClick = playPlaylist,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onDeletePlaylist = ::deletePlaylist
                )
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun deletePlaylist(id: Long) {
        coroutineScope.launch {
            playlistRepo.delete(id)
        }
    }

    private fun showAddToPlaylistDialog(playlistId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Playlist(playlistId),
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

    private sealed class PlaylistListState {
        data object Loading : PlaylistListState()

        data class Loaded(
            val playlists: StateFlow<List<PlaylistListItem>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onPlaylistClick: (Long) -> Unit,
            val onPlayPlaylistClick: (Long) -> Unit,
            val onAddToPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit,
            val onDeletePlaylist: (id: Long) -> Unit
        ) : PlaylistListState() {
            data class PlaylistListItem(
                val id: Long,
                val name: String,
                val image: ByteArray?,
            )
        }
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: PlaylistListState) {
            when (state) {
                is PlaylistListState.Loading -> Text(modifier = modifier, text = "Loading...")
                is PlaylistListState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: PlaylistListState.Loaded) {
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier = modifier,
                playlists = state.playlists,
                onPlaylistClick = state.onPlaylistClick,
                onPlayPlaylistClick = state.onPlayPlaylistClick,
                onAddToPlaylistClick = state.onAddToPlaylistClick,
                onDeletePlaylistClick = state.onDeletePlaylist
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
            playlists: StateFlow<List<PlaylistListState.Loaded.PlaylistListItem>>,
            onPlaylistClick: (Long) -> Unit,
            onPlayPlaylistClick: (Long) -> Unit,
            onAddToPlaylistClick: (id: Long) -> Unit,
            onDeletePlaylistClick: (id: Long) -> Unit
        ) {
            val items by playlists.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        VerticalScrollbar(lazyGridState)
                        LazyVerticalGrid (
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyGridState,
                            contentPadding = PaddingValues(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            columns = GridCells.Adaptive(200.dp)
                        ) {
                            items(items = items, key = { it.id }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                    onAddToPlaylistClick = { onAddToPlaylistClick(playlist.id) },
                                    onDeleteClick = { onDeletePlaylistClick(playlist.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) }
            )
        }

        @Composable
        private fun PlaylistItem(
            modifier: Modifier = Modifier,
            playlist: PlaylistListState.Loaded.PlaylistListItem,
            onClick: () -> Unit,
            onPlayClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onDeleteClick: () -> Unit
        ) {
            Item (
                modifier = modifier,
                onClick = onClick
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        data = playlist.image,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            content = { Icon(Icons.Default.Delete, null) },
                            onClick = onDeleteClick
                        )
                        IconButton(
                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, null) },
                            onClick = onAddToPlaylistClick
                        )
                        IconButton(
                            content = { Icon(Icons.Default.PlayCircle, null) },
                            onClick = onPlayClick
                        )
                    }
                }
            }
        }
    }
}