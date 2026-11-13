package dev.younesgouyd.apps.music.client.multiplatform.components

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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.QueueItemParameter
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistList(
    playlistRepo: PlaylistRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    artistRepo: SpotifyArtistRepo,
    albumRepo: SpotifyAlbumRepo,
    mediaController: MediaController,
    showPlaylistDetails: (PlaylistId) -> Unit
) : Component() {
    override val title: String = "Playlists"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")

    init {
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                playlists = searchQuery.flatMapLatest { nameQuery ->
                    playlistRepo.search(nameQuery).mapLatest { list ->
                        list.map { dbPlaylist ->
                            Ui.State.Loaded.PlaylistListItem(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = null
                            )
                        }
                    }
                }.stateIn(coroutineScope),
                addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                addToPlaylist = addToPlaylist.asStateFlow(),
                searchQuery = searchQuery.asStateFlow(),
                onSearchQueryChange = { searchQuery.value = it },
                onPlaylistClick = showPlaylistDetails,
                onPlayPlaylist = { id: PlaylistId -> mediaController.playQueue(listOf(QueueItemParameter.Playlist(id))) },
                onAddToPlaylist = { playlistId: PlaylistId ->
                    addToPlaylist.update {
                        AddToPlaylist(
                            itemToAdd = AddToPlaylist.Item.Playlist(playlistId),
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
                onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                onDeletePlaylist = { id: PlaylistId ->
                    coroutineScope.launch { playlistRepo.delete(id) }
                },
                onRenamePlaylist = { newName: String, id: PlaylistId ->
                    coroutineScope.launch {
                        playlistRepo.updateName(id = id, name = newName)
                    }
                },
                onAddPlaylistToQueue = { id: PlaylistId -> mediaController.addToQueue(listOf(
                    QueueItemParameter.Playlist(id))) }
            )
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
                val playlists: StateFlow<List<PlaylistListItem>>,
                val addToPlaylistDialogVisible: StateFlow<Boolean>,
                val addToPlaylist: StateFlow<Component?>,
                val searchQuery: StateFlow<String>,
                val onSearchQueryChange: (String) -> Unit,
                val onPlaylistClick: (PlaylistId) -> Unit,
                val onPlayPlaylist: (PlaylistId) -> Unit,
                val onAddToPlaylist: (PlaylistId) -> Unit,
                val onDismissAddToPlaylistDialog: () -> Unit,
                val onDeletePlaylist: (PlaylistId) -> Unit,
                val onRenamePlaylist: (newName: String, id: PlaylistId) -> Unit,
                val onAddPlaylistToQueue: (PlaylistId) -> Unit
            ) : State() {
                data class PlaylistListItem(
                    val id: PlaylistId,
                    val name: String,
                    val image: MediaFileId?,
                )
            }
        }

        @Composable
        private fun RenamePlaylistForm(
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
                            text = "Rename playlist",
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

        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Text(modifier = modifier, text = "Loading...")
                    is State.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: State.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    playlists = state.playlists,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onPlaylistClick = state.onPlaylistClick,
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
                playlists: StateFlow<List<State.Loaded.PlaylistListItem>>,
                searchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onPlaylistClick: (PlaylistId) -> Unit,
                onPlayPlaylist: (PlaylistId) -> Unit,
                onAddToPlaylist: (PlaylistId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onRenamePlaylist: (newName: String, id: PlaylistId) -> Unit,
                onAddPlaylistToQueue: (id: PlaylistId) -> Unit
            ) {
                val items by playlists.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val gridState = rememberLazyGridState()
                val minColumnWidth: Dp = when (getWindowSizeClass()) {
                    WindowWidthSizeClass.Compact -> 100.dp
                    else -> 200.dp
                }

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                label = { Text("Search") },
                                value = searchQuery,
                                onValueChange = onSearchQueryChange
                            )
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                state = gridState,
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(minColumnWidth)
                            ) {
                                items(items = items, key = { it.id.value }) { playlist ->
                                    PlaylistItem(
                                        modifier = Modifier.aspectRatio(0.65f),
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
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
                    floatingActionButton = { ScrollToTopFloatingActionButton(gridState) }
                )
            }

            @Composable
            private fun PlaylistItem(
                modifier: Modifier,
                playlist: State.Loaded.PlaylistListItem,
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

                Item(
                    modifier = modifier,
                    onClick = onClick
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            id = playlist.image,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
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
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = playlist.name,
                            image = playlist.image
                        ),
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }

                if (showEditFormDialog) {
                    RenamePlaylistForm(
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
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Text(modifier = modifier, text = "Loading...")
                    is State.Loaded -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: State.Loaded) {
                val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
                val addToPlaylist by state.addToPlaylist.collectAsState()

                Main(
                    modifier = modifier,
                    playlists = state.playlists,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onPlaylistClick = state.onPlaylistClick,
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
                playlists: StateFlow<List<State.Loaded.PlaylistListItem>>,
                searchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onPlaylistClick: (PlaylistId) -> Unit,
                onPlayPlaylist: (PlaylistId) -> Unit,
                onAddToPlaylist: (PlaylistId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onRenamePlaylist: (newName: String, id: PlaylistId) -> Unit,
                onAddPlaylistToQueue: (PlaylistId) -> Unit
            ) {
                val items by playlists.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val gridState = rememberLazyGridState()
                val minColumnWidth: Dp = when (getWindowSizeClass()) {
                    WindowWidthSizeClass.Compact -> 100.dp
                    else -> 200.dp
                }

                Scaffold(
                    modifier = modifier,
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                label = { Text("Search") },
                                value = searchQuery,
                                onValueChange = onSearchQueryChange
                            )
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                state = gridState,
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(minColumnWidth)
                            ) {
                                items(items = items, key = { it.id.value }) { playlist ->
                                    PlaylistItem(
                                        modifier = Modifier.aspectRatio(0.65f),
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
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
                    floatingActionButton = {
                        ScrollToTopFloatingActionButton(
                            gridState
                        )
                    }
                )
            }

            @Composable
            private fun PlaylistItem(
                modifier: Modifier,
                playlist: State.Loaded.PlaylistListItem,
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

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            id = playlist.image,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = playlist.name,
                            image = playlist.image
                        ),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = { onPlayClick(); showContextMenu = false },
                        )
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }

                if (showEditFormDialog) {
                    RenamePlaylistForm(
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
        }
    }
}