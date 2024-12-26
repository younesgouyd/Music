package dev.younesgouyd.apps.music.app.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
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
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.Item
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.*
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Folder
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Playlist
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.awt.FileDialog
import java.awt.Frame
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MusicLibrary(
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    artistRepo: ArtistRepo,
    albumRepo: AlbumRepo,
) : Component() {
    override val title: String = "Music Library"
    private val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    private val path: StateFlow<Set<Folder>>
    private val folders: StateFlow<List<Folder>>
    private val playlists: StateFlow<List<Playlist>>
    private val tracks: StateFlow<List<Track>>
    private val loadingItems: StateFlow<Boolean>
    private val loadingFolders: MutableStateFlow<Boolean>
    private val loadingPlaylists: MutableStateFlow<Boolean>
    private val loadingTracks: MutableStateFlow<Boolean>

    private val playerController = PlayerController(
        coroutineScope = coroutineScope,
        trackRepo = trackRepo,
        artistRepo = artistRepo,
        albumRepo = albumRepo,
        playlistRepo = playlistRepo,
        onAlbumClick = { TODO() },
        onArtistClick = { TODO() }
    )
    private val player = Player(
        playerController = playerController,
        showAlbumDetails = { TODO() },
        showArtistDetails = { TODO() }
    )
    private val queue = Queue(playerController)

    init {
        loadingFolders = MutableStateFlow(true)
        loadingPlaylists = MutableStateFlow(true)
        loadingTracks = MutableStateFlow(true)
        loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks) { loading1, loading2, loading3 ->
            loading1 || loading2 || loading3
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = true)

        path = flow {
            var value: Set<Folder> = emptySet()
            currentFolder.collect { folder ->
                if (folder == null) {
                    value = emptySet()
                    emit(value)
                } else {
                    val list = value.takeWhile { it.id != folder.id }.toMutableList()
                    list.add(folder)
                    value = list.toSet()
                    emit(value)
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptySet())

        folders = currentFolder.flatMapLatest {
            flow {
                loadingFolders.value = true
                folderRepo.getSubfolders(it?.id).collect {
                    emit(it)
                    loadingFolders.value = false
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        playlists = currentFolder.flatMapLatest {
            flow {
                emit(emptyList())
                loadingPlaylists.value = true
                if (it != null) {
                    playlistRepo.getFolderPlaylists(it.id).collect { playlist ->
                        emit(playlist)
                        loadingPlaylists.value = false
                    }
                }
                loadingPlaylists.value = false
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        tracks = currentFolder.flatMapLatest {
            flow {
                emit(emptyList())
                loadingTracks.value = true
                if (it != null) {
                    trackRepo.getFolderTracks(it.id).collect { tracks ->
                        emit(tracks)
                        loadingTracks.value = false
                    }
                }
                loadingTracks.value = false
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())
    }

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(
            path = path,
            loadingItems = loadingItems,
            currentFolder = currentFolder.asStateFlow(),
            folders = folders,
            playlists = playlists,
            tracks = tracks,
            player = player,
            queue = queue,
            onNewFolder = ::addFolder,
            onNewTrack = ::addTrack,
            onFolderClick = ::openFolder,
            onPlaylistClick = { TODO() },
            onTrackClick = { coroutineScope.launch { playerController.play(listOf(PlayerController.QueueItemParameter.Track(it))) } },
            onRenameFolder = ::renameFolder,
            onRenamePlaylist = ::renamePlaylist,
            onDeleteFolder = ::deleteFolder,
            onDeletePlaylist = ::deletePlaylist,
            onDeleteTrack = ::deleteTrack,
            onAddTrackToQueue = ::addTrackToQueue
        )
    }

    override fun clear() {
        playerController.release()
        coroutineScope.cancel()
    }

    private fun openFolder(folder: Folder?) {
        currentFolder.value = folder
    }

    private fun addFolder(name: String) {
        coroutineScope.launch {
            folderRepo.add(name = name, parentFolderId = currentFolder.value?.id)
        }
    }

    private fun addTrack(name: String, audioUrl: String?, videoUrl: String?) {
        coroutineScope.launch {
            trackRepo.add(
                name = name,
                folderId = currentFolder.value!!.id,
                albumId = null,
                audioUrl = audioUrl,
                videoUrl = videoUrl
            )
        }
    }

    private fun renameFolder(id: Long, name: String) {
        coroutineScope.launch {
            folderRepo.updateName(id = id, name = name)
        }
    }

    private fun renamePlaylist(id: Long, name: String) {
        coroutineScope.launch {
            playlistRepo.updateName(id = id, name = name)
        }
    }

    private fun deleteFolder(id: Long) {
        coroutineScope.launch {
            folderRepo.delete(id)
        }
    }

    private fun deletePlaylist(id: Long) {
        coroutineScope.launch {
            playlistRepo.delete(id)
        }
    }

    private fun deleteTrack(id: Long) {
        coroutineScope.launch {
            trackRepo.delete(id)
        }
    }

    private fun addTrackToQueue(id: Long) {
        coroutineScope.launch {
            playerController.addToQueue(PlayerController.QueueItemParameter.Track(id))
        }
    }

    private object Ui {
        @Composable
        fun Main(
            path: StateFlow<Set<Folder>>,
            loadingItems: StateFlow<Boolean>,
            currentFolder: StateFlow<Folder?>,
            folders: StateFlow<List<Folder>>,
            playlists: StateFlow<List<Playlist>>,
            tracks: StateFlow<List<Track>>,
            player: Component,
            queue: Component,
            onNewFolder: (name: String) -> Unit,
            onNewTrack: (name: String, audioUrl: String?, videoUrl: String?) -> Unit,
            onFolderClick: (Folder?) -> Unit,
            onPlaylistClick: (id: Long) -> Unit,
            onTrackClick: (Long) -> Unit,
            onRenameFolder: (Long, name: String) -> Unit,
            onRenamePlaylist: (id: Long, name: String) -> Unit,
            onDeleteFolder: (Long) -> Unit,
            onDeletePlaylist: (id: Long) -> Unit,
            onDeleteTrack: (Long) -> Unit,
            onAddTrackToQueue: (Long) -> Unit
        ) {
            val path by path.collectAsState()
            val loadingItems by loadingItems.collectAsState()
            val folders by folders.collectAsState()
            val playlists by playlists.collectAsState()
            val tracks by tracks.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(weight = .8f)
                ) {
                    Scaffold(
                        modifier = Modifier.weight(weight = .7f),
                        floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.size(12.dp))
                            ToolBar(
                                modifier = Modifier.fillMaxWidth(),
                                currentFolder = currentFolder,
                                path = path,
                                onFolderClick = onFolderClick,
                                onNewFolder = onNewFolder,
                                onNewTrack = onNewTrack
                            )
                            Box(modifier = Modifier) {
                                VerticalScrollbar(lazyGridState)
                                LazyVerticalGrid(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    state = lazyGridState,
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                    columns = GridCells.Adaptive(200.dp)
                                ) {
                                    items(items = folders, key = { it.id }) { folder ->
                                        FolderItem(
                                            folder = folder,
                                            onClick = { onFolderClick(folder) },
                                            onRename = { onRenameFolder(folder.id, it) },
                                            onDeleteClick = { onDeleteFolder(folder.id) }
                                        )
                                    }
                                    items(items = playlists, key = { it.id }) { playlist ->
                                        PlaylistItem(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) },
                                            onRename = { onRenamePlaylist(playlist.id, it) },
                                            onDeleteClick = { onDeletePlaylist(playlist.id) }
                                        )
                                    }
                                    items(items = tracks, key = { it.id }) {
                                        TrackItem(
                                            track = it,
                                            onClick = { onTrackClick(it.id) },
                                            onDeleteClick = { onDeleteTrack(it.id) },
                                            onAddToQueueClick = { onAddTrackToQueue(it.id) }
                                        )
                                    }
                                    if (loadingItems) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(50.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    queue.show(Modifier.weight(.3f))
                }
                player.show(Modifier.weight(.2f))
            }
        }

        @Composable
        private fun ToolBar(
            modifier: Modifier = Modifier,
            currentFolder: StateFlow<Folder?>,
            path: Set<Folder>,
            onFolderClick: (Folder?) -> Unit,
            onNewFolder: (name: String) -> Unit,
            onNewTrack: (name: String, audioUrl: String?, videoUrl: String?) -> Unit
        ) {
            val currentFolder by currentFolder.collectAsState()
            val pathLazyListState = rememberLazyListState()
            var newFolderFormVisible by remember { mutableStateOf(false) }
            var newTrackFormVisible by remember { mutableStateOf(false) }

            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    LazyRow(
                        state = pathLazyListState,
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    content = { Text(".") },
                                    onClick = { onFolderClick(null) }
                                )
                                Text("/")
                            }
                        }
                        items(items = path.toList(), key = { it.id }) { folder ->
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    content = { Text(folder.name) },
                                    onClick = { onFolderClick(folder) }
                                )
                                Text("/")
                            }
                        }
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { /* todo - sort by alpha */ },
                            content = { Icon(Icons.Default.SortByAlpha, null) }
                        )
                        IconButton(
                            onClick = { /* todo - sort by date */ },
                            content = { Icon(Icons.AutoMirrored.Default.Sort, null) }
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { newFolderFormVisible = true },
                            content = { Icon(Icons.Default.CreateNewFolder, null) }
                        )
                        if (currentFolder != null) {
                            IconButton(
                                onClick = { newTrackFormVisible = true },
                                content = { Icon(Icons.Default.Audiotrack, null) }
                            )
                        }
                    }
                }
            }

            if (newFolderFormVisible) {
                val dismiss = { newFolderFormVisible = false }
                Dialog(onDismissRequest = dismiss) {
                    FolderForm(
                        title = "New folder",
                        onDone = { onNewFolder(it); dismiss() }
                    )
                }
            }

            if (newTrackFormVisible) {
                val dismiss = { newTrackFormVisible = false }
                Dialog(onDismissRequest = dismiss) {
                    TrackForm(
                        title = "New Track",
                        onDone = { name: String, audioUrl: String?, videoUrl: String? ->
                            onNewTrack(name, audioUrl, videoUrl)
                            dismiss()
                        }
                    )
                }
            }

            LaunchedEffect(path) {
                pathLazyListState.animateScrollToItem(path.size)
            }
        }

        @Composable
        private fun FolderItem(
            modifier: Modifier = Modifier,
            folder: Folder,
            onClick: () -> Unit,
            onRename: (name: String) -> Unit,
            onDeleteClick: () -> Unit
        ) {
            var deleteConfirmationDialogVisible by remember { mutableStateOf(false) }
            var editFormDialogVisible by remember { mutableStateOf(false) }

            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        imageVector = Icons.Default.Folder,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { editFormDialogVisible = true },
                            content = { Icon(Icons.Default.Edit, null) }
                        )
                        IconButton(
                            onClick = { deleteConfirmationDialogVisible = true },
                            content = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }

            if (editFormDialogVisible) {
                val dismiss = { editFormDialogVisible = false }
                Dialog(onDismissRequest = dismiss) {
                    FolderForm(
                        title = "Rename folder",
                        name = folder.name,
                        onDone = { onRename(it); dismiss() }
                    )
                }
            }

            if (deleteConfirmationDialogVisible) {
                DeleteConfirmationDialog(
                    message = "Delete \"${folder.name}\"?",
                    onDismissRequest = { deleteConfirmationDialogVisible = false },
                    onYesClick = onDeleteClick
                )
            }
        }

        @Composable
        private fun PlaylistItem(
            modifier: Modifier = Modifier,
            playlist: Playlist,
            onClick: () -> Unit,
            onRename: (name: String) -> Unit,
            onDeleteClick: () -> Unit
        ) {
            var deleteConfirmationDialogVisible by remember { mutableStateOf(false) }
            var editFormDialogVisible by remember { mutableStateOf(false) }

            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        imageVector = Icons.Default.Folder,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter,
                        contentDescription = null
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
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { editFormDialogVisible = true },
                            content = { Icon(Icons.Default.Edit, null) }
                        )
                        IconButton(
                            onClick = { deleteConfirmationDialogVisible = true },
                            content = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }

            if (editFormDialogVisible) {
                val dismiss = { editFormDialogVisible = false }
                Dialog(onDismissRequest = dismiss) {
                    PlaylistForm(
                        title = "Rename playlist",
                        name = playlist.name,
                        onDone = { onRename(it); dismiss() }
                    )
                }
            }

            if (deleteConfirmationDialogVisible) {
                DeleteConfirmationDialog(
                    message = "Delete playlist \"${playlist.name}\"?",
                    onDismissRequest = { deleteConfirmationDialogVisible = false },
                    onYesClick = onDeleteClick
                )
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            track: Track,
            onClick: () -> Unit,
            onDeleteClick: () -> Unit,
            onAddToQueueClick: () -> Unit
        ) {
            var deleteConfirmationDialogVisible by remember { mutableStateOf(false) }

            Item(modifier = modifier, onClick = onClick) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        url = null,
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
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { deleteConfirmationDialogVisible = true },
                            content = { Icon(Icons.Default.Delete, null) }
                        )
                        IconButton(
                            onClick = onAddToQueueClick,
                            content = { Icon(Icons.Default.AddToQueue, null) }
                        )
                    }
                }
            }

            if (deleteConfirmationDialogVisible) {
                DeleteConfirmationDialog(
                    message = "Delete \"${track.name}\"?",
                    onDismissRequest = { deleteConfirmationDialogVisible = false },
                    onYesClick = onDeleteClick
                )
            }
        }

        @Composable
        private fun FolderForm(
            title: String,
            name: String = "",
            onDone: (name: String) -> Unit
        ) {
            var name by remember { mutableStateOf(name) }

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

        @Composable
        private fun PlaylistForm(
            title: String,
            name: String = "",
            onDone: (name: String) -> Unit
        ) {
            var name by remember { mutableStateOf(name) }

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

        @Composable
        private fun TrackForm(
            title: String,
            name: String = "",
            onDone: (name: String, audioUrl: String?, videoUrl: String?) -> Unit
        ) {
            var name by remember { mutableStateOf(name) }
            var audioUrl: String? by remember { mutableStateOf(null) }
            var videoUrl: String? by remember { mutableStateOf(null) }

            fun getFilePathFromSystemFilePicker(): String? {
                val fileDialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD)
                fileDialog.isVisible = true
                return fileDialog.file?.let { fileDialog.directory + it }
            }

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
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Audio File Path") },
                        value = audioUrl ?: "",
                        readOnly = true,
                        onValueChange = {},
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                content = { Icon(Icons.Default.AudioFile, null) },
                                onClick = { audioUrl = getFilePathFromSystemFilePicker() }
                            )
                        }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Video File Path") },
                        value = videoUrl ?: "",
                        readOnly = true,
                        onValueChange = {},
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                content = { Icon(Icons.Default.VideoFile, null) },
                                onClick = { videoUrl = getFilePathFromSystemFilePicker() }
                            )
                        }
                    )
                    Button(
                        content = { Text("Done") },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDone(name, audioUrl, videoUrl) }
                    )
                }
            }
        }

        @Composable
        private fun DeleteConfirmationDialog(
            message: String,
            onDismissRequest: () -> Unit,
            onYesClick: () -> Unit
        ) {
            Dialog(onDismissRequest = onDismissRequest) {
                Surface(
                    modifier = Modifier.width(500.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                content = { Text("Yes") },
                                onClick = onYesClick
                            )
                            Button(
                                content = { Text("No") },
                                onClick = onDismissRequest
                            )
                        }
                    }
                }
            }
        }
    }

    private class PlayerController(
        private val coroutineScope: CoroutineScope,
        private val trackRepo: TrackRepo,
        private val artistRepo: ArtistRepo,
        private val albumRepo: AlbumRepo,
        private val playlistRepo: PlaylistRepo,
        private val onAlbumClick: (Long) -> Unit,
        private val onArtistClick: (Long) -> Unit
    ) {
        private val mutex = Mutex()
        private val _state: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.Unavailable)
        private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
        private val vlcPlayer = AudioPlayerComponent().mediaPlayer()

        val state: StateFlow<PlayerState> get() = _state.asStateFlow()

        init {
            NativeDiscovery().discover()
        }

        suspend fun play(queue: List<QueueItemParameter>) {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> {
                            if (queue.isEmpty()) { TODO() }
                            PlayerState.Loading
                        }
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().pause() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> {
                            val newQueue = queue.map { it.toModel() }
                            newQueue.first().let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                                }
                                if (currentTrack.audio?.url != null) {
                                    vlcPlayer.media().start(currentTrack.audio.url)
                                }
                            }
                            PlayerState.Available(
                                enabled = enabled.asStateFlow(),
                                playbackState = PlayerState.Available.PlaybackState(
                                    queue = newQueue,
                                    queueItemIndex = 0,
                                    queueSubItemIndex = 0,
                                    isPlaying = true,
                                    repeatState = PlayerState.Available.PlaybackState.RepeatState.Off,
                                    duration = vlcPlayer.media().info().duration().milliseconds,
                                    elapsedTime = Duration.ZERO,
                                    audioOrVideoState = PlayerState.Available.PlaybackState.AudioOrVideoState.Audio
                                ),
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onValueChange = { coroutineScope.launch { seek(it) } },
                                onPreviousClick = { coroutineScope.launch { previous() } },
                                onPlayClick = { coroutineScope.launch { play(it) } },
                                onPauseClick = { coroutineScope.launch { pause() } },
                                onNextClick = { coroutineScope.launch { next() } },
                                onCompleted = { coroutineScope.launch { next() } }, // todo
                                onRepeatClick = { coroutineScope.launch { repeat() } }, // TODO
                                onPlayQueueItem = { coroutineScope.launch { playQueueItem(it) } },
                                onPlayQueueSubItem = { queueItemIndex: Int, trackIndex: Int ->
                                    coroutineScope.launch { playTrackInQueue(queueItemIndex, trackIndex) }
                                }
                            )
                        }
                        is PlayerState.Available -> {
                            if (queue.isEmpty()) {
                                if (!currentState.playbackState.isPlaying) { vlcPlayer.controls().play() }
                                currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = true))
                            } else {
                                val newQueue = queue.map { it.toModel() }
                                newQueue.first().let { firstQueueItem ->
                                    val currentTrack = when (firstQueueItem) {
                                        is PlayerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                        is PlayerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                        is PlayerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                                    }
                                    if (currentTrack.audio?.url != null) {
                                        vlcPlayer.media().start(currentTrack.audio.url)
                                    }
                                }
                                currentState.copy(
                                    playbackState = currentState.playbackState.copy(
                                        queue = newQueue,
                                        queueItemIndex = 0,
                                        queueSubItemIndex = 0,
                                        isPlaying = true,
                                        duration = vlcPlayer.media().info().duration().milliseconds
                                    )
                                )
                            }
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun pause() {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().pause() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun seek(position: Duration) {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            vlcPlayer.controls().setTime(position.inWholeMilliseconds)
                            currentState.copy(playbackState = currentState.playbackState.copy(elapsedTime = position))
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun next() {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            val queue = currentState.playbackState.queue
                            val currentIndex = currentState.playbackState.queueItemIndex
                            val currentSubIndex = currentState.playbackState.queueSubItemIndex
                            val newIndex: Int
                            val newSubIndex: Int
                            queue[currentIndex].let { queueItem ->
                                when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> {
                                        newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                        newSubIndex = 0
                                    }
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> {
                                        if (currentSubIndex + 1 > queueItem.items.size - 1) {
                                            newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                            newSubIndex = 0
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex + 1
                                        }
                                    }
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> {
                                        if (currentSubIndex + 1 > queueItem.items.size - 1) {
                                            newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                            newSubIndex = 0
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex + 1
                                        }
                                    }
                                }
                            }
                            val newTrack = queue[newIndex].let { queueItem ->
                                when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[newSubIndex]
                                }
                            }
                            if (newTrack.audio?.url != null) {
                                vlcPlayer.media().start(newTrack.audio.url)
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = newIndex,
                                    queueSubItemIndex = newSubIndex,
                                    isPlaying = true,
                                    duration = vlcPlayer.media().info().duration().milliseconds
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun previous() {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            val queue = currentState.playbackState.queue
                            val currentIndex = currentState.playbackState.queueItemIndex
                            val currentSubIndex = currentState.playbackState.queueSubItemIndex
                            val newIndex: Int
                            val newSubIndex: Int
                            queue[currentIndex].let { queueItem ->
                                when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> {
                                        newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                        newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                            when (it) {
                                                is PlayerState.Available.PlaybackState.QueueItem.Track -> 0
                                                is PlayerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                                is PlayerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
                                            }
                                        }
                                    }
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> {
                                        if (currentSubIndex - 1 < 0) {
                                            newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                            newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                                when (it) {
                                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> 0
                                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
                                                }
                                            }
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex - 1
                                        }
                                    }
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> {
                                        if (currentSubIndex - 1 < 0) {
                                            newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                            newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                                when (it) {
                                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> 0
                                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
                                                }
                                            }
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex - 1
                                        }
                                    }
                                }
                            }
                            val newTrack = queue[newIndex].let { queueItem ->
                                when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[newSubIndex]
                                }
                            }
                            if (newTrack.audio?.url != null) {
                                vlcPlayer.media().start(newTrack.audio.url)
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = newIndex,
                                    queueSubItemIndex = newSubIndex,
                                    isPlaying = true,
                                    duration = vlcPlayer.media().info().duration().milliseconds
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun repeat() {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    repeatState = when (currentState.playbackState.repeatState) {
                                        PlayerState.Available.PlaybackState.RepeatState.Off -> PlayerState.Available.PlaybackState.RepeatState.Track
                                        PlayerState.Available.PlaybackState.RepeatState.Track -> PlayerState.Available.PlaybackState.RepeatState.List
                                        PlayerState.Available.PlaybackState.RepeatState.List -> PlayerState.Available.PlaybackState.RepeatState.Off
                                    }
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        suspend fun addToQueue(item: QueueItemParameter) {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> PlayerState.Loading
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> currentState
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> {
                            val newQueue = listOf(item.toModel())
                            newQueue.first().let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                                }
                                if (currentTrack.audio?.url != null) {
                                    vlcPlayer.media().startPaused(currentTrack.audio.url)
                                }
                            }
                            PlayerState.Available(
                                enabled = enabled.asStateFlow(),
                                playbackState = PlayerState.Available.PlaybackState(
                                    queue = newQueue,
                                    queueItemIndex = 0,
                                    queueSubItemIndex = 0,
                                    isPlaying = false,
                                    repeatState = PlayerState.Available.PlaybackState.RepeatState.Off,
                                    duration = vlcPlayer.media().info().duration().milliseconds,
                                    elapsedTime = Duration.ZERO,
                                    audioOrVideoState = PlayerState.Available.PlaybackState.AudioOrVideoState.Audio
                                ),
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onValueChange = { coroutineScope.launch { seek(it) } },
                                onPreviousClick = { coroutineScope.launch { previous() } },
                                onPlayClick = { coroutineScope.launch { play(it) } },
                                onPauseClick = { coroutineScope.launch { pause() } },
                                onNextClick = { coroutineScope.launch { next() } },
                                onCompleted = { coroutineScope.launch { next() } }, // todo
                                onRepeatClick = { coroutineScope.launch { repeat() } }, // TODO
                                onPlayQueueItem = { coroutineScope.launch { playQueueItem(it) } },
                                onPlayQueueSubItem = { queueItemIndex: Int, trackIndex: Int ->
                                    coroutineScope.launch { playTrackInQueue(queueItemIndex, trackIndex) }
                                }
                            )
                        }
                        is PlayerState.Available -> {
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queue = currentState.playbackState.queue.toMutableList().apply { add(item.toModel()) }
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        private suspend fun playQueueItem(queueItemIndex: Int) {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            currentState.playbackState.queue[queueItemIndex].let { queueItem ->
                                val newTrack = when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items.first()
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> queueItem.items.first()
                                }
                                if (newTrack.audio?.url != null) {
                                    vlcPlayer.media().start(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = queueItemIndex,
                                    queueSubItemIndex = 0,
                                    isPlaying = true,
                                    duration = vlcPlayer.media().info().duration().milliseconds,
                                    elapsedTime = Duration.ZERO
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        private suspend fun playTrackInQueue(queueItemIndex: Int, trackIndex: Int) {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is PlayerState.Unavailable -> TODO()
                        is PlayerState.Loading -> TODO()
                        is PlayerState.Available -> {
                            currentState.playbackState.queue[queueItemIndex].let { queueItem ->
                                val newTrack = when (queueItem) {
                                    is PlayerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                    is PlayerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[trackIndex]
                                    is PlayerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[trackIndex]
                                }
                                if (newTrack.audio?.url != null) {
                                    vlcPlayer.media().start(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = queueItemIndex,
                                    queueSubItemIndex = trackIndex,
                                    duration = vlcPlayer.media().info().duration().milliseconds,
                                    elapsedTime = Duration.ZERO
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }

        private suspend fun QueueItemParameter.toModel(): PlayerState.Available.PlaybackState.QueueItem {
            return when (this) {
                is QueueItemParameter.Track -> trackRepo.getStatic(this.id).let { dbTrack ->
                    PlayerState.Available.PlaybackState.QueueItem.Track(
                        id = dbTrack.id,
                        name = dbTrack.name,
                        artists = artistRepo.getTrackArtistsStatic(this.id)
                            .map { dbArtist ->
                                PlayerState.Available.PlaybackState.QueueItem.Track.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = dbArtist.image
                                )
                            },
                        album = dbTrack.album_id?.let {
                            albumRepo.getStatic(it).let { dbAlbum ->
                                PlayerState.Available.PlaybackState.QueueItem.Track.Album(
                                    id = dbAlbum.id,
                                    name = dbAlbum.name,
                                    image = dbAlbum.image,
                                    releaseDate = dbAlbum.release_date
                                )
                            }
                        },
                        audio = dbTrack.audio_url?.let {
                            PlayerState.Available.PlaybackState.QueueItem.Track.Audio(
                                url = it.replaceFirst("file:/", "file:///"),
                                duration = getDuration(it)
                            )
                        },
                        video = dbTrack.video_url?.let {
                            PlayerState.Available.PlaybackState.QueueItem.Track.Video(
                                url = it.replaceFirst("file:/", "file:///"),
                                duration = getDuration(it)
                            )
                        }
                    )
                }
                is QueueItemParameter.Album -> albumRepo.getStatic(this.id).let { dbAlbum ->
                    PlayerState.Available.PlaybackState.QueueItem.Album(
                        id = dbAlbum.id,
                        name = dbAlbum.name,
                        image = dbAlbum.image,
                        releaseDate = dbAlbum.release_date,
                        items = trackRepo.getArtistTracksStatic(dbAlbum.id).map { dbTrack ->
                            PlayerState.Available.PlaybackState.QueueItem.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name,
                                        image = dbArtist.image
                                    )
                                },
                                album = PlayerState.Available.PlaybackState.QueueItem.Track.Album(
                                    id = dbAlbum.id,
                                    name = dbAlbum.name,
                                    image = dbAlbum.image,
                                    releaseDate = dbAlbum.release_date
                                ),
                                audio = dbTrack.audio_url?.let {
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Audio(
                                        url = it.replaceFirst("file:/", "file:///"),
                                        duration = getDuration(it)
                                    )
                                },
                                video = dbTrack.video_url?.let {
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Video(
                                        url = it.replaceFirst("file:/", "file:///"),
                                        duration = getDuration(it)
                                    )
                                }
                            )
                        }
                    )
                }
                is QueueItemParameter.Playlist -> playlistRepo.getStatic(this.id).let { dbPlaylist ->
                    PlayerState.Available.PlaybackState.QueueItem.Playlist(
                        id = dbPlaylist.id,
                        name = dbPlaylist.name,
                        image = dbPlaylist.image,
                        items = trackRepo.getPlaylistTracksStatic(dbPlaylist.id).map { dbTrack ->
                            PlayerState.Available.PlaybackState.QueueItem.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name,
                                        image = dbArtist.image
                                    )
                                },
                                album = dbTrack.album_id?.let {
                                    albumRepo.getStatic(it).let { dbAlbum ->
                                        PlayerState.Available.PlaybackState.QueueItem.Track.Album(
                                            id = dbAlbum.id,
                                            name = dbAlbum.name,
                                            image = dbAlbum.image,
                                            releaseDate = dbAlbum.release_date
                                        )
                                    }
                                },
                                audio = dbTrack.audio_url?.let {
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Audio(
                                        url = it.replaceFirst("file:/", "file:///"),
                                        duration = getDuration(it)
                                    )
                                },
                                video = dbTrack.video_url?.let {
                                    PlayerState.Available.PlaybackState.QueueItem.Track.Video(
                                        url = it.replaceFirst("file:/", "file:///"),
                                        duration = getDuration(it)
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }

        private suspend fun getDuration(mediaPath: String): Duration {
            return withContext(Dispatchers.IO) {
                val factory = MediaPlayerFactory()
                val media = factory.media().newMedia(mediaPath)
                try {
                    if (media.parsing().parse()) {
                        media.info().duration().milliseconds
                    } else {
                        TODO()
                    }
                } finally {
                    media.release()
                    factory.release()
                }
            }
        }

        fun release() {
            vlcPlayer.release()
        }

        sealed class QueueItemParameter {
            abstract val id: Long

            data class Track(
                override val id: Long
            ) : QueueItemParameter()

            data class Playlist(
                override val id: Long,
                val orderBy: OrderBy
            ): QueueItemParameter() {
                data class OrderBy(
                    val order: Order,
                    val asc: Boolean
                ) {
                    enum class Order { AddedDate, Custom }
                }
            }

            data class Album(
                override val id: Long
            ): QueueItemParameter()
        }

        sealed class PlayerState {
            data object Loading : PlayerState()

            data object Unavailable : PlayerState()

            data class Available(
                val enabled: StateFlow<Boolean>,
                val playbackState: PlaybackState,
                val onAlbumClick: (Long) -> Unit,
                val onArtistClick: (Long) -> Unit,
                val onValueChange: (Duration) -> Unit,
                val onPreviousClick: () -> Unit,
                val onPlayClick: (queue: List<QueueItemParameter>) -> Unit,
                val onPauseClick: () -> Unit,
                val onNextClick: () -> Unit,
                val onCompleted: () -> Unit,
                val onRepeatClick: (PlaybackState.RepeatState) -> Unit,
                val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
                val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) ->Unit
            ) : PlayerState() {
                data class PlaybackState(
                    val queue: List<QueueItem>,
                    val queueItemIndex: Int,
                    val queueSubItemIndex: Int,
                    val isPlaying: Boolean,
                    val repeatState: RepeatState,
                    val duration: Duration,
                    val elapsedTime: Duration,
                    val audioOrVideoState: AudioOrVideoState
                ) {
                    val currentTrack get() = when (val it = queue[queueItemIndex]) {
                        is QueueItem.Track -> it
                        is QueueItem.Playlist -> it.items[queueSubItemIndex]
                        is QueueItem.Album -> it.items[queueSubItemIndex]
                    }

                    init { if (queue.isEmpty()) { TODO() } }

                    sealed class QueueItem {
                        data class Track(
                            val id: Long,
                            val name: String,
                            val artists: List<Artist>,
                            val album: Album?,
                            val audio: Audio?,
                            val video: Video?
                        ) : QueueItem() {
                            val playable = audio?.url != null

                            data class Artist(
                                val id: Long,
                                val name: String,
                                val image: ByteArray?
                            )

                            data class Album(
                                val id: Long,
                                val name: String,
                                val image: ByteArray?,
                                val releaseDate: String?
                            )

                            data class Audio(
                                val url: String,
                                val duration: Duration
                            )

                            data class Video(
                                val url: String,
                                val duration: Duration
                            )
                        }

                        data class Playlist(
                            val id: Long,
                            val name: String,
                            val image: ByteArray?,
                            val items: List<Track>
                        ): QueueItem() {
                            init { if (items.isEmpty()) { TODO() } }
                        }

                        data class Album(
                            val id: Long,
                            val name: String,
                            val image: ByteArray?,
                            val releaseDate: String?,
                            val items: List<Track>
                        ): QueueItem() {
                            init { if (items.isEmpty()) { TODO() } }
                        }
                    }

                    enum class RepeatState { Off, Track, List }

                    enum class AudioOrVideoState { Audio, Video }
                }
            }
        }
    }


    private class PlaylistDetails(
        private val id: Long,
        private val trackRepo: TrackRepo,
        private val playlistRepo: PlaylistRepo,
        private val artistRepo: ArtistRepo,
        private val albumRepo: AlbumRepo,
        showArtistDetails: (id: Long) -> Unit,
        showAlbumDetails: (id: Long) -> Unit,
        play: () -> Unit,
        playTrack: (id: Long) -> Unit,
    ) : Component() {
        override val title: String = "Playlist"
        private val state: MutableStateFlow<PlaylistDetailsState> = MutableStateFlow(PlaylistDetailsState.Loading)

        init {
            coroutineScope.launch {
                state.update {
                    PlaylistDetailsState.State(
                        playlist = playlistRepo.get(id).mapLatest { dbPlaylist ->
                            PlaylistDetailsState.State.Playlist(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = dbPlaylist.image
                            )
                        }.stateIn(coroutineScope),
                        tracks = trackRepo.getPlaylistTracks(id).mapLatest {
                            it.map { dbTrack ->
                                PlaylistDetailsState.State.Track(
                                    id = dbTrack.id,
                                    name = dbTrack.name,
                                    artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                        PlaylistDetailsState.State.Track.Artist(
                                            id = dbArtist.id,
                                            name = dbArtist.name
                                        )
                                    },
                                    album = dbTrack.album_id?.let {
                                        albumRepo.getStatic(it).let { dbAlbum ->
                                            PlaylistDetailsState.State.Track.Album(
                                                id = dbAlbum.id,
                                                name = dbAlbum.name,
                                                image = dbAlbum.image
                                            )
                                        }
                                    },
                                    addedAt = Instant.ofEpochMilli(dbTrack.added_at).toString()
                                )
                            }
                        }.stateIn(coroutineScope),
                        onPlayClick = play,
                        onTrackClick = playTrack,
                        onArtistClick = showArtistDetails,
                        onAlbumClick = showAlbumDetails
                    )
                }
            }
        }

        @Composable
        override fun show(modifier: Modifier) {
            val state by state.collectAsState()

            Ui.Main(state)
        }

        override fun clear() {
            coroutineScope.cancel()
        }

        sealed class PlaylistDetailsState {
            data object Loading : PlaylistDetailsState()

            data class State(
                val playlist: StateFlow<Playlist>,
                val tracks: StateFlow<List<Track>>,
                val onPlayClick: () -> Unit,
                val onTrackClick: (id: Long) -> Unit,
                val onArtistClick: (id: Long) -> Unit,
                val onAlbumClick: (id: Long) -> Unit
            ) : PlaylistDetailsState() {
                data class Playlist(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?
                )

                data class Track(
                    val id: Long,
                    val name: String,
                    val artists: List<Artist>,
                    val album: Album?,
                    val addedAt: String
                ) {
                    data class Artist(
                        val id: Long,
                        val name: String
                    )

                    data class Album(
                        val id: Long,
                        val name: String,
                        val image: ByteArray?
                    )
                }
            }
        }

        private object Ui {
            @Composable
            fun Main(state: PlaylistDetailsState) {
                when (state) {
                    is PlaylistDetailsState.Loading -> Text("Loading...")
                    is PlaylistDetailsState.State -> Main(state)
                }
            }

            @Composable
            private fun Main(state: PlaylistDetailsState.State) {
                Main(
                    playlist = state.playlist,
                    tracks = state.tracks,
                    onPlayClick = state.onPlayClick,
                    onTrackClick = state.onTrackClick,
                    onArtistClick = state.onArtistClick,
                    onAlbumClick = state.onAlbumClick
                )
            }

            @OptIn(ExperimentalFoundationApi::class)
            @Composable
            private fun Main(
                playlist: StateFlow<PlaylistDetailsState.State.Playlist>,
                tracks: StateFlow<List<PlaylistDetailsState.State.Track>>,
                onPlayClick: () -> Unit,
                onTrackClick: (id: Long) -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onAlbumClick: (id: Long) -> Unit
            ) {
                val playlist by playlist.collectAsState()
                val items by tracks.collectAsState()
                val lazyColumnState = rememberLazyListState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        Box(modifier = Modifier.fillMaxSize().padding(it)) {
                            VerticalScrollbar(lazyColumnState)
                            LazyColumn (
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                                state = lazyColumnState,
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    PlaylistInfo(
                                        modifier = Modifier.fillMaxWidth().height(400.dp),
                                        playlist = playlist,
                                        onPlayClick = onPlayClick
                                    )
                                }
                                item {
                                    Spacer(Modifier.size(8.dp))
                                }
                                stickyHeader {
                                    TracksHeader(modifier = Modifier.fillMaxWidth().height(64.dp))
                                    HorizontalDivider()
                                }
                                items(items = items) { track ->
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        track = track,
                                        onTrackClick = onTrackClick,
                                        onArtistClick = onArtistClick,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(lazyColumnState) }
                )
            }

            @Composable
            private fun PlaylistInfo(
                modifier: Modifier,
                playlist: PlaylistDetailsState.State.Playlist,
                onPlayClick: () -> Unit
            ) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.fillMaxHeight(),
                        url = null, // TODO
                        contentScale = ContentScale.FillHeight
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = playlist.name,
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
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
                        }
                    }
                }
            }

            private const val TITLE_WEIGHT = .45f
            private const val ALBUM_WEIGHT = .18f
            private const val ADDED_AT_WEIGHT = .1f
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
                        Box(modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Date added",
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
                track: PlaylistDetailsState.State.Track,
                onTrackClick: (id: Long) -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onAlbumClick: (id: Long) -> Unit
            ) {
                Row(
                    modifier = modifier.clickable { onTrackClick(track.id) },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // image + title + artists
                    Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.CenterStart) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.fillMaxHeight(),
                                url = null, // TODO
                                contentScale = ContentScale.FillHeight
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = track.name ?: "",
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
                    }

                    Spacer(Modifier.width(8.dp))

                    // album
                    Box(modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT), contentAlignment = Alignment.CenterStart) {
                        if (track.album != null) {
                            TextButton(
                                onClick = { onAlbumClick(track.album.id) },
                                content = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Album, null)
                                        Text(
                                            text = track.album.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // added at
                    Box(modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = track.addedAt ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // actions
                    Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.Center) {
                        Row {
                            IconButton(
                                content = { Icon(Icons.Default.Save, null) },
                                onClick = { /* TODO: addToPlaylistDialogVisible */ }
                            )
                            IconButton(
                                content = { Icon(Icons.Default.Folder, null) },
                                onClick = { /* TODO: addToFolderDialogVisible */ }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }


    private class Player(
        private val playerController: PlayerController,
        private val showAlbumDetails: (Long) -> Unit,
        private val showArtistDetails: (Long) -> Unit
    ) : Component() {
        override val title: String = ""
        private val state: StateFlow<PlayerController.PlayerState>
            get() = playerController.state

        @Composable
        override fun show(modifier: Modifier) {
            val state by state.collectAsState()

            Ui.Main(modifier = modifier, state = state)
        }

        override fun clear() {
            coroutineScope.cancel()
        }

        private object Ui {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: PlayerController.PlayerState) {
                when (state) {
                    is PlayerController.PlayerState.Loading -> Unit
                    is PlayerController.PlayerState.Unavailable -> Unit
                    is PlayerController.PlayerState.Available -> Main(modifier = modifier, state = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier = Modifier, state: PlayerController.PlayerState.Available) {
                Main(
                    modifier =  modifier,
                    enabled = state.enabled,
                    playbackState = state.playbackState,
                    onAlbumClick = state.onAlbumClick,
                    onArtistClick = state.onArtistClick,
                    onValueChange = state.onValueChange,
                    onPreviousClick = state.onPreviousClick,
                    onPlayClick = { state.onPlayClick(emptyList()) },
                    onPauseClick = state.onPauseClick,
                    onNextClick = state.onNextClick,
                    onCompleted = state.onCompleted,
                    onRepeatClick = state.onRepeatClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                playbackState: PlayerController.PlayerState.Available.PlaybackState,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onValueChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onCompleted: () -> Unit,
                onRepeatClick: (PlayerController.PlayerState.Available.PlaybackState.RepeatState) -> Unit
            ) {
                val duration: Duration = playbackState.duration
                val animateableElapsedTime = remember { animateableOf(0.milliseconds) }

                Main(
                    modifier = modifier,
                    enabled = enabled,
                    playbackState = playbackState.copy(elapsedTime = animateableElapsedTime.value),
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onValueChange = onValueChange,
                    onPreviousClick = onPreviousClick,
                    onPlayClick = onPlayClick,
                    onPauseClick = onPauseClick,
                    onNextClick = onNextClick,
                    onRepeatClick = onRepeatClick
                )

                LaunchedEffect(duration) {
                    animateableElapsedTime.stop()
                    animateableElapsedTime.snapTo(0.milliseconds)
                    animateableElapsedTime.updateBounds(lowerBound = 0.milliseconds, upperBound = duration)
                    if (playbackState.isPlaying) {
                        animateableElapsedTime.animateTo(
                            targetValue = duration,
                            animationSpec = linearAnimation(duration)
                        )
                        onCompleted()
                    }
                }

                LaunchedEffect(playbackState.isPlaying) {
                    if (playbackState.isPlaying) {
                        animateableElapsedTime.animateTo(
                            targetValue = duration,
                            animationSpec = linearAnimation(duration - animateableElapsedTime.value)
                        )
                        onCompleted()
                    } else {
                        animateableElapsedTime.stop()
                    }
                }

                LaunchedEffect(playbackState.elapsedTime, playbackState.isPlaying) {
                    animateableElapsedTime.snapTo(playbackState.elapsedTime)
                    if (playbackState.isPlaying) {
                        animateableElapsedTime.animateTo(
                            targetValue = duration,
                            animationSpec = linearAnimation(duration - playbackState.elapsedTime)
                        )
                        onCompleted()
                    }
                }
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                enabled: StateFlow<Boolean>,
                playbackState: PlayerController.PlayerState.Available.PlaybackState,
                onAlbumClick: (Long) -> Unit,
                onArtistClick: (Long) -> Unit,
                onValueChange: (Duration) -> Unit,
                onPreviousClick: () -> Unit,
                onPlayClick: () -> Unit,
                onPauseClick: () -> Unit,
                onNextClick: () -> Unit,
                onRepeatClick: (PlayerController.PlayerState.Available.PlaybackState.RepeatState) -> Unit
            ) {
                val enabled by enabled.collectAsState()
                val duration: Duration = playbackState.duration

                Card(
                    modifier = modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.elevatedCardElevation(),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.size(170.dp),
                            url = null // todo
                        )
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = playbackState.currentTrack.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                content = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Album, null)
                                        Text(
                                            text = playbackState.currentTrack.album?.name ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    playbackState.currentTrack.album?.let {
                                        onAlbumClick(it.id)
                                    }
                                }
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (artist in playbackState.currentTrack.artists) {
                                    TextButton(
                                        content = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Person, null)
                                                Text(
                                                    text = artist.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${playbackState.elapsedTime.formatted()}/${duration.formatted()}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IconButton(
                                    content = { Icon(Icons.Default.SkipPrevious, null) },
                                    enabled = enabled,
                                    onClick = onPreviousClick
                                )
                                when (playbackState.isPlaying) {
                                    true -> IconButton(
                                        content = { Icon(Icons.Default.PauseCircle, null) },
                                        enabled = enabled,
                                        onClick = onPauseClick
                                    )
                                    false -> IconButton(
                                        content = { Icon(Icons.Default.PlayCircle, null) },
                                        enabled = enabled,
                                        onClick = onPlayClick
                                    )
                                }
                                IconButton(
                                    content = { Icon(Icons.Default.SkipNext, null) },
                                    enabled = enabled,
                                    onClick = onNextClick
                                )
                                when (playbackState.repeatState) {
                                    PlayerController.PlayerState.Available.PlaybackState.RepeatState.Off -> {
                                        IconButton(
                                            enabled = enabled,
                                            onClick = { onRepeatClick(PlayerController.PlayerState.Available.PlaybackState.RepeatState.List) },
                                            content = { Icon(Icons.Default.Repeat, null) }
                                        )
                                    }
                                    PlayerController.PlayerState.Available.PlaybackState.RepeatState.List -> {
                                        IconButton(
                                            enabled = enabled,
                                            onClick = { onRepeatClick(PlayerController.PlayerState.Available.PlaybackState.RepeatState.Track) },
                                            content = { Icon(Icons.Default.RepeatOn, null) }
                                        )
                                    }
                                    PlayerController.PlayerState.Available.PlaybackState.RepeatState.Track -> {
                                        IconButton(
                                            enabled = enabled,
                                            onClick = { onRepeatClick(PlayerController.PlayerState.Available.PlaybackState.RepeatState.Off) },
                                            content = { Icon(Icons.Default.RepeatOneOn, null) }
                                        )
                                    }
                                }
                                Slider(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = enabled,
                                    value = playbackState.elapsedTime.inWholeMilliseconds.toFloat(),
                                    valueRange = 0f..duration.inWholeMilliseconds.toFloat(),
                                    onValueChange = { onValueChange(it.toLong().milliseconds) }
                                )
                            }
                        }
                    }
                }
            }

            private fun animateableOf(
                initialValue: Duration
            ): Animatable<Duration, AnimationVector1D> {
                return Animatable(
                    initialValue = initialValue,
                    typeConverter = TwoWayConverter(
                        convertToVector = { AnimationVector1D(it.inWholeMilliseconds.toFloat()) },
                        convertFromVector = { it.value.toLong().milliseconds }
                    )
                )
            }

            private fun linearAnimation(duration: Duration): TweenSpec<Duration> {
                return tween(
                    durationMillis = duration.inWholeMilliseconds.toInt(),
                    easing = LinearEasing
                )
            }

            fun Duration?.formatted(): String {
                return this?.let {
                    it.toComponents { minutes, seconds, _ ->
                        minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
                    }
                } ?: ""
            }
        }
    }

    private class Queue(
        private val playerController: PlayerController
    ) : Component() {
        override val title: String = "Queue"
        private val state: StateFlow<PlayerController.PlayerState>
            get() = playerController.state

        @Composable
        override fun show(modifier: Modifier) {
            val state by state.collectAsState()

            Ui.Main(modifier = modifier, state = state)
        }

        override fun clear() {
            coroutineScope.cancel()
        }

        private object Ui {
            @Composable
            fun Main(modifier: Modifier = Modifier, state: PlayerController.PlayerState) {
                when (state) {
                    is PlayerController.PlayerState.Loading -> Unit
                    is PlayerController.PlayerState.Unavailable -> Unit
                    is PlayerController.PlayerState.Available -> Main(modifier = modifier, playerState = state)
                }
            }

            @OptIn(ExperimentalFoundationApi::class)
            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                playerState: PlayerController.PlayerState.Available
            ) {
                val enabled by playerState.enabled.collectAsState()

                Card(
                    modifier = modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(),
                    colors = CardDefaults.elevatedCardColors(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        stickyHeader {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Default.QueueMusic, null)
                                Text(
                                    text = "Queue",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        itemsIndexed(
                            items = playerState.playbackState.queue
                        ) { index: Int, queueItem: PlayerController.PlayerState.Available.PlaybackState.QueueItem ->
                            when (queueItem) {
                                is PlayerController.PlayerState.Available.PlaybackState.QueueItem.Track -> {
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        onClick = { playerState.onPlayQueueItem(index) }
                                    )
                                }
                                is PlayerController.PlayerState.Available.PlaybackState.QueueItem.Playlist -> {
                                    PlaylistItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        onTrackClick = { trackIndex -> playerState.onPlayQueueSubItem(index, trackIndex) }
                                    )
                                }
                                is PlayerController.PlayerState.Available.PlaybackState.QueueItem.Album -> {
                                    AlbumItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        onTrackClick = { trackIndex -> playerState.onPlayQueueSubItem(index, trackIndex) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier = Modifier,
                item: PlayerController.PlayerState.Available.PlaybackState.QueueItem.Track,
                enabled: Boolean,
                onClick: () -> Unit
            ) {
                Card(
                    modifier = modifier,
                    enabled = enabled,
                    onClick = onClick
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Audiotrack, null)
                        Image(
                            modifier = Modifier.size(80.dp),
                            url = null // todo
                        )
                        Text(
                            text = if (item.artists.isEmpty()) item.name else "${item.name} - ${item.artists.first().name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            @Composable
            private fun PlaylistItem(
                modifier: Modifier = Modifier,
                item: PlayerController.PlayerState.Available.PlaybackState.QueueItem.Playlist,
                enabled: Boolean,
                onTrackClick: (index: Int) -> Unit
            ) {
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = modifier,
                    onClick = { expanded = !expanded }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Default.QueueMusic, null)
                        Image(
                            modifier = Modifier.size(80.dp),
                            url = null // todo
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            when (expanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        if (expanded) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = item.items
                                ) { index: Int, item: PlayerController.PlayerState.Available.PlaybackState.QueueItem.Track ->
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = item,
                                        enabled = enabled,
                                        onClick = { onTrackClick(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun AlbumItem(
                modifier: Modifier = Modifier,
                item: PlayerController.PlayerState.Available.PlaybackState.QueueItem.Album,
                enabled: Boolean,
                onTrackClick: (index: Int) -> Unit
            ) {
                var expanded by remember { mutableStateOf(false) }

                Surface(
                    modifier = modifier,
                    onClick = { expanded = !expanded }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Album, null)
                        Image(
                            modifier = Modifier.size(80.dp),
                            url = null // todo
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            when (expanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        if (expanded) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = item.items
                                ) { index: Int, item: PlayerController.PlayerState.Available.PlaybackState.QueueItem.Track ->
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = item,
                                        enabled = enabled,
                                        onClick = { onTrackClick(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
