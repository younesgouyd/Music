package dev.younesgouyd.apps.music.desktop.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.common.components.PlaylistList
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.desktop.components.util.widgets.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PlaylistList(
    playlistRepo: PlaylistRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    albumRepo: AlbumRepo,
    mediaController: MediaController,
    showPlaylistDetails: (id: Long) -> Unit
) : PlaylistList(
    playlistRepo, playlistTrackCrossRefRepo, trackRepo, folderRepo, albumRepo, mediaController,
    showPlaylistDetails
) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun showAddToPlaylistDialog(playlistId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Playlist(playlistId),
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
                onPlaylist = state.onPlaylist,
                onPlayPlaylist = state.onPlayPlaylist,
                onAddToPlaylist = state.onAddToPlaylist,
                onDeletePlaylist = state.onDeletePlaylist,
                onRenamePlaylist = state.onRenamePlaylist,
                onAddPlaylistToQueue = state.onAddPlaylistToQueue
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
            onPlaylist: (Long) -> Unit,
            onPlayPlaylist: (Long) -> Unit,
            onAddToPlaylist: (id: Long) -> Unit,
            onDeletePlaylist: (id: Long) -> Unit,
            onRenamePlaylist: (newName: String, id: Long) -> Unit,
            onAddPlaylistToQueue: (id: Long) -> Unit
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
                                    onClick = { onPlaylist(playlist.id) },
                                    onPlayClick = { onPlayPlaylist(playlist.id) },
                                    onAddToPlaylistClick = { onAddToPlaylist(playlist.id) },
                                    onDeleteClick = { onDeletePlaylist(playlist.id) },
                                    onRenameClick = { newName -> onRenamePlaylist(newName, playlist.id) },
                                    onAddToQueueClick = { onAddPlaylistToQueue(playlist.id) }
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
            onDeleteClick: () -> Unit,
            onRenameClick: (newName: String) -> Unit,
            onAddToQueueClick: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
            var showEditFormDialog by remember { mutableStateOf(false) }

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
                }
            }

            if (showContextMenu) {
                ItemContextMenu(
                    item = Item(name = playlist.name, image = playlist.image),
                    onDismiss = { showContextMenu = false }
                ) {
                    Option(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteConfirmationDialog = true },
                    )
                    Option(
                        label = "Rename",
                        icon = Icons.Default.Edit,
                        onClick = { showEditFormDialog = true },
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
                    Option(
                        label = "Play next",
                        icon = Icons.Default.QueuePlayNext,
                        onClick = { TODO() },
                    )
                }
            }

            if (showEditFormDialog) {
                PlaylistForm(
                    title = "Rename playlist",
                    name = playlist.name,
                    onDone = { onRenameClick(it); showEditFormDialog = false },
                    onDismiss = { showEditFormDialog = false }
                )
            }

            if (showDeleteConfirmationDialog) {
                DeleteConfirmationDialog(
                    message = "Delete playlist \"${playlist.name}\"?",
                    onDismissRequest = { showDeleteConfirmationDialog = false },
                    onYesClick = {
                        showDeleteConfirmationDialog = false
                        showContextMenu = false
                        onDeleteClick()
                    }
                )
            }
        }

        @Composable
        private fun PlaylistForm(
            title: String,
            name: String = "",
            onDone: (name: String) -> Unit,
            onDismiss: () -> Unit
        ) {
            var name by remember { mutableStateOf(name) }

            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier.width(500.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name") },
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onDone(name) }),
                        )
                        Button(
                            content = { Text("Done") },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onDone(name) }
                        )
                    }
                }
            }
        }
    }
}