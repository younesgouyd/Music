package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.common.components.util.compose.SystemFilePicker
import dev.younesgouyd.apps.music.common.components.util.compose.widgets.*
import dev.younesgouyd.apps.music.common.data.Server
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.room.entities.Folder
import dev.younesgouyd.apps.music.common.data.room.entities.Playlist
import dev.younesgouyd.apps.music.common.scanFolder
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class Library(
    private val server: Server,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: ArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val importSessionWithItemsRepo: ImportSessionWithItemsRepo,
    private val mediaController: MediaController,
    private val showPlaylist: (id: Long) -> Unit,
    private val showArtistDetails: (id: Long) -> Unit
) : Component() {
    override val title: String = "Library"
    private val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    private val path: StateFlow<List<Models.NodeState>>
    private val folders: StateFlow<List<Folder>>
    private val playlists: StateFlow<List<Playlist>>
    private val tracks: StateFlow<List<Models.Track>>
    private val loadingItems: StateFlow<Boolean>
    private val loadingFolders: MutableStateFlow<Boolean>
    private val loadingPlaylists: MutableStateFlow<Boolean>
    private val loadingTracks: MutableStateFlow<Boolean>
    private val importingFolder: MutableStateFlow<Boolean>
    private val addToPlaylistDialogVisible = MutableStateFlow(false)
    private val inspectionDialogVisible = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val inspection: MutableStateFlow<Inspection?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")

    init {
        loadingFolders = MutableStateFlow(true)
        loadingPlaylists = MutableStateFlow(true)
        loadingTracks = MutableStateFlow(true)
        importingFolder = MutableStateFlow(false)
        loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks, importingFolder) { loading1, loading2, loading3, loading4 ->
            loading1 || loading2 || loading3 || loading4
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = true)

        val root = Models.NodeState(null, LazyGridState())
        var list: List<Models.NodeState> = listOf(root)
        path = flow {
            fun <T> List<T>.takeUntil(predicate: (T) -> Boolean): List<T> {
                val list = mutableListOf<T>()
                for (item in this) {
                    list.add(item)
                    if (predicate(item)) {
                        break
                    }
                }
                return list
            }
            currentFolder.collect { folder ->
                if (folder == null) {
                    list = listOf(root)
                    emit(list)
                } else {
                    val temp = list.takeUntil { it.folder?.id == folder.id }.toMutableList()
                    if (!temp.any { it.folder?.id == folder.id }) {
                        temp.add(Models.NodeState(folder, LazyGridState()))
                    }
                    list = temp
                    emit(list)
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = list)

        folders = currentFolder.flatMapLatest { parentFolder ->
            loadingFolders.value = true
            searchQuery.flatMapLatest { search ->
                folderRepo.searchFolder(parentFolder?.id, search)
                    .also {
                        loadingFolders.value = false
                    }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        playlists = currentFolder.flatMapLatest { parentFolder ->
            searchQuery.flatMapLatest { search ->
                loadingPlaylists.value = true
                playlistRepo.searchFolder(parentFolder?.id, search)
                    .also {
                        loadingFolders.value = false
                    }
                }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        tracks = currentFolder.flatMapLatest { parentFolder ->
            loadingTracks.value = true
            searchQuery.flatMapLatest { search ->
                trackRepo.searchFolder(parentFolder?.id, search).map { tracks ->
                    tracks.map { dbTrack ->
                        Models.Track(
                            id = dbTrack.id,
                            name = dbTrack.name,
                            image = dbTrack.albumArt?.let { Base64.decode(it) },
                            artists = artistRepo.getTrackArtists(dbTrack.id).first().map {
                                Models.Track.Artist(id = it.id, name = it.name)
                            }
                        )
                    }
                }.also {
                    loadingFolders.value = false
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())
    }

    @Composable
    override fun show(modifier: Modifier) {
        val inspectionDialogVisible by inspectionDialogVisible.collectAsState()
        val addToPlaylistDialogVisible by addToPlaylistDialogVisible.collectAsState()
        var importTypeDialogVisible by remember { mutableStateOf(false) }
        var preparingImportDialogVisible by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val inspection by inspection.collectAsState()
        val addToPlaylist by addToPlaylist.collectAsState()

        if (importTypeDialogVisible) {
            Ui.Common.ImportFormDialog(
                onFolderPicked = {
                    coroutineScope.launch {
                        importTypeDialogVisible = false
                        preparingImportDialogVisible = true
                        importFolder(it)
                        preparingImportDialogVisible = false
                    }
                },
                onUrlEntered = {
                    importTypeDialogVisible = false
                    showInspectionDialog(it)
                },
                onDismiss = { importTypeDialogVisible = false }
            )
        }
        if (preparingImportDialogVisible) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier.size(500.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Please wait, preparing import...")
                    }
                }
            }
        }
        if (inspectionDialogVisible) {
            Dialog(onDismissRequest = ::dismissInspectionDialog) {
                inspection!!.show(Modifier)
            }
        }
        if (addToPlaylistDialogVisible) {
            Dialog(onDismissRequest = ::dismissAddToPlaylistDialog) {
                addToPlaylist!!.show(Modifier)
            }
        }
        AdaptiveUi(
            wide = {
                Ui.Wide.Main(
                    modifier = modifier,
                    path = path,
                    loadingItems = loadingItems,
                    currentFolder = currentFolder.asStateFlow(),
                    folders = folders,
                    playlists = playlists,
                    tracks = tracks,
                    onImportClick = { importTypeDialogVisible = true },
                    onNewFolder = {
                        coroutineScope.launch {
                            folderRepo.add(
                                name = it,
                                parentFolderId = currentFolder.value?.id
                            )
                        }
                    },
                    onFolderClick = { currentFolder.value = it },
                    onAddFolderToPlaylistClick = ::showAddFolderToPlaylistDialog,
                    onAddFolderToQueueClick = ::addFolderToQueue,
                    onPlayFolder = ::playFolder,
                    onPlaylistClick = showPlaylist,
                    onPlayPlaylistClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(it))) },
                    onAddPlaylistToPlaylistClick = ::showAddPlaylistToPlaylistDialog,
                    onAddPlaylistToQueueClick = {
                        mediaController.addToQueue(
                            listOf(MediaController.QueueItemParameter.Playlist(it))
                        )
                    },
                    onTrackClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onArtistClick = showArtistDetails,
                    onRenameFolder = { id: Long, name: String ->
                        coroutineScope.launch {
                            folderRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onRenamePlaylist = { id: Long, name: String ->
                        coroutineScope.launch {
                            playlistRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onDeleteFolder = { coroutineScope.launch { folderRepo.delete(it) } },
                    onDeletePlaylist = { coroutineScope.launch { playlistRepo.delete(it) } },
                    onDeleteTrack = { coroutineScope.launch { trackRepo.delete(it) } },
                    onAddTrackToQueue = { mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onRenameTrack = { id: Long, name: String ->
                        coroutineScope.launch {
                            trackRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onMoveFolderToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            folderRepo.updateParentFolderId(
                                id = id,
                                parentFolderId = folderId
                            )
                        }
                    },
                    onMoveTrackToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            trackRepo.updateFolderId(
                                id = id,
                                folderId = folderId
                            )
                        }
                    },
                    onMovePlaylistToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            playlistRepo.updateFolderId(
                                id = id,
                                folderId = folderId
                            )
                        }
                    }
                )
            },
            compact = {
                Ui.Compact.Main(
                    modifier = modifier,
                    path = path,
                    loadingItems = loadingItems,
                    currentFolder = currentFolder.asStateFlow(),
                    folders = folders,
                    playlists = playlists,
                    tracks = tracks,
                    onImportClick = { importTypeDialogVisible = true },
                    onNewFolder = {
                        coroutineScope.launch {
                            folderRepo.add(
                                name = it,
                                parentFolderId = currentFolder.value?.id
                            )
                        }
                    },
                    onFolderClick = { currentFolder.value = it },
                    onAddFolderToPlaylistClick = ::showAddFolderToPlaylistDialog,
                    onAddFolderToQueueClick = ::addFolderToQueue,
                    onPlayFolder = ::playFolder,
                    onPlaylistClick = showPlaylist,
                    onPlayPlaylistClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(it))) },
                    onAddPlaylistToPlaylistClick = ::showAddPlaylistToPlaylistDialog,
                    onAddPlaylistToQueueClick = {
                        mediaController.addToQueue(
                            listOf(MediaController.QueueItemParameter.Playlist(it))
                        )
                    },
                    onTrackClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onArtistClick = showArtistDetails,
                    onRenameFolder = { id: Long, name: String ->
                        coroutineScope.launch {
                            folderRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onRenamePlaylist = { id: Long, name: String ->
                        coroutineScope.launch {
                            playlistRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onDeleteFolder = { coroutineScope.launch { folderRepo.delete(it) } },
                    onDeletePlaylist = { coroutineScope.launch { playlistRepo.delete(it) } },
                    onDeleteTrack = { coroutineScope.launch { trackRepo.delete(it) } },
                    onAddTrackToQueue = { mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onRenameTrack = { id: Long, name: String ->
                        coroutineScope.launch {
                            trackRepo.updateName(
                                id = id,
                                name = name
                            )
                        }
                    },
                    onMoveFolderToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            folderRepo.updateParentFolderId(
                                id = id,
                                parentFolderId = folderId
                            )
                        }
                    },
                    onMoveTrackToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            trackRepo.updateFolderId(
                                id = id,
                                folderId = folderId
                            )
                        }
                    },
                    onMovePlaylistToFolder = { id: Long, folderId: Long ->
                        coroutineScope.launch {
                            playlistRepo.updateFolderId(
                                id = id,
                                folderId = folderId
                            )
                        }
                    }
                )
            }
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun importFolder(uri: String) {
        val items = scanFolder(uri)
        importSessionWithItemsRepo.addLocalSession(
            inspection = dev.younesgouyd.apps.music.common.Inspection.Folder(
                container = dev.younesgouyd.apps.music.common.Inspection.ContainerInspection.Folder(uri = uri),
                items = items
            )
        )
    }

    private fun importUrl(url: String, inspection: dev.younesgouyd.apps.music.common.Inspection.Webpage, selected: List<Long>) {
        coroutineScope.launch {
            importSessionWithItemsRepo.addUrlSession(url, inspection, selected)
        }
    }

    private fun playFolder(folderId: Long) {
        suspend fun getFolderItems(_folderId: Long): List<MediaController.QueueItemParameter> {
            val tracks = trackRepo.getFolderTracks(_folderId).first().map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
            val playlists = playlistRepo.getFolderPlaylists(_folderId).first().map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
            return tracks + playlists + folderRepo.getSubfolders(_folderId).first().flatMap { getFolderItems(it.id) }
        }
        coroutineScope.launch {
            val queue = getFolderItems(folderId)
            mediaController.playQueue(queue)
        }
    }

    private fun showAddTrackToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Track(trackId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.value = false
    }

    private fun showAddPlaylistToPlaylistDialog(playlistId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Playlist(playlistId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.value = false
    }

    private fun showAddFolderToPlaylistDialog(folderId: Long) {
        addToPlaylist.update {
            if (it != null) TODO()
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Folder(folderId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.value = false
    }

    private fun showInspectionDialog(url: String) {
        inspection.update {
            if (it != null) TODO()
            Inspection(
                server = server,
                url = url,
                onDone = { inspection, selected ->
                    importUrl(url, inspection, selected)
                    dismissInspectionDialog()
                }
            )
        }
        inspectionDialogVisible.value = true
    }

    private fun addFolderToQueue(id: Long) {
        suspend fun getFolderItems(_id: Long): List<MediaController.QueueItemParameter> {
            val tracks = trackRepo.getFolderTracks(_id).first().map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
            val playlists = playlistRepo.getFolderPlaylists(_id).first().map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
            return tracks + playlists + folderRepo.getSubfolders(_id).first().flatMap { getFolderItems(it.id) }
        }
        coroutineScope.launch {
            val queue = getFolderItems(id)
            mediaController.addToQueue(queue)
        }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it!!.clear(); null }
    }

    private fun dismissInspectionDialog() {
        inspectionDialogVisible.value = false
        inspection.update { it!!.clear(); null }
    }

    private object Models {
        data class NodeState(
            val folder: Folder?,
            val scrollState: LazyGridState,
        )
        data class Track(
            val id: Long,
            val name: String,
            val image: ByteArray?,
            val artists: List<Artist>
        ) {
            data class Artist(
                val id: Long,
                val name: String
            )
        }
    }

    private object Ui {
        object Common {
            @Composable
            fun ImportFormDialog(
                onFolderPicked: (uri: String) -> Unit,
                onUrlEntered: (url: String) -> Unit,
                onDismiss: () -> Unit
            ) {
                var showSystemFilePicker: Boolean by remember { mutableStateOf(false) }
                val (url, setUrl) = remember { mutableStateOf("") }

                Dialog(onDismissRequest = onDismiss ) {
                    Surface(
                        modifier = Modifier.width(500.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Select source type")
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Import from local folder")
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { showSystemFilePicker = true },
                                        content = { Text("Open System File Picker") }
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Import from a url")
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Enter the URL of a song or a playlist") },
                                        value = url,
                                        onValueChange = setUrl
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (url.isBlank()) {
                                                TODO()
                                            } else {
                                                onUrlEntered(url)
                                            }
                                        },
                                        content = { Text("Import from URL") }
                                    )
                                }
                            }
                        }
                    }
                }
                if (showSystemFilePicker) {
                    SystemFilePicker(onFolderPicked)
                }
            }
        }

        object Wide {
            @Composable
            fun Main(
                modifier: Modifier = Modifier,
                path: StateFlow<List<Models.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                currentFolder: StateFlow<Folder?>,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<Playlist>>,
                tracks: StateFlow<List<Models.Track>>,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onFolderClick: (Folder?) -> Unit,
                onAddFolderToPlaylistClick: (id: Long) -> Unit,
                onAddFolderToQueueClick: (id: Long) -> Unit,
                onPlayFolder: (id: Long) -> Unit,
                onPlaylistClick: (id: Long) -> Unit,
                onPlayPlaylistClick: (id: Long) -> Unit,
                onAddPlaylistToPlaylistClick: (id: Long) -> Unit,
                onAddPlaylistToQueueClick: (id: Long) -> Unit,
                onTrackClick: (id: Long) -> Unit,
                onAddTrackToPlaylistClick: (id: Long) -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onRenameFolder: (id: Long, name: String) -> Unit,
                onRenamePlaylist: (id: Long, name: String) -> Unit,
                onDeleteFolder: (id: Long) -> Unit,
                onDeletePlaylist: (id: Long) -> Unit,
                onDeleteTrack: (id: Long) -> Unit,
                onAddTrackToQueue: (id: Long) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit,
                onRenameTrack: (id: Long, name: String) -> Unit,
                onMoveFolderToFolder: (id: Long, folderId: Long) -> Unit,
                onMoveTrackToFolder: (id: Long, folderId: Long) -> Unit,
                onMovePlaylistToFolder: (id: Long, folderId: Long) -> Unit
            ) {
                val path by path.collectAsState()
                val loadingItems by loadingItems.collectAsState()
                val folders by folders.collectAsState()
                val playlists by playlists.collectAsState()
                val tracks by tracks.collectAsState()
                val scrollState = remember(path.last().folder?.id) {
                    path.last().scrollState
                }
                Scaffold(
                    modifier = modifier,
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) },
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.size(12.dp))
                            ToolBar(
                                modifier = Modifier.fillMaxWidth(),
                                currentFolder = currentFolder,
                                path = path.mapNotNull { it.folder },
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder
                            )
                            Box(modifier = Modifier) {
                                LazyVerticalGrid(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    state = scrollState,
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                    columns = GridCells.Adaptive(200.dp)
                                ) {
                                    items(folders, { "folder#${it.id}"}) { folder ->
                                        FolderItem(
                                            folder = folder,
                                            onClick = { onFolderClick(folder) },
                                            onAddToPlaylistClick = { onAddFolderToPlaylistClick(folder.id) },
                                            onAddToQueueClick = { onAddFolderToQueueClick(folder.id) },
                                            onPlayClick = { onPlayFolder(folder.id) },
                                            onRenameClick = { onRenameFolder(folder.id, it) },
                                            onDeleteClick = { onDeleteFolder(folder.id) },
                                            onMoveToFolder = { onMoveFolderToFolder(folder.id, it) }
                                        )
                                    }
                                    items(playlists, { "playlist#${it.id}" }) { playlist ->
                                        PlaylistItem(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) },
                                            onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                            onAddToPlaylistClick = { onAddPlaylistToPlaylistClick(playlist.id) },
                                            onAddToQueueClick = { onAddPlaylistToQueueClick(playlist.id) },
                                            onRenameClick = { onRenamePlaylist(playlist.id, it) },
                                            onDeleteClick = { onDeletePlaylist(playlist.id) },
                                            onMoveToFolder = { onMovePlaylistToFolder(playlist.id, it) }
                                        )
                                    }
                                    items(tracks, { "track#${it.id}" }) { track ->
                                        TrackItem(
                                            track = track,
                                            onClick = { onTrackClick(track.id) },
                                            onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                            onArtistClick = onArtistClick,
                                            onDeleteClick = { onDeleteTrack(track.id) },
                                            onAddToQueueClick = { onAddTrackToQueue(track.id) },
                                            onRenameClick = { onRenameTrack(track.id, it) },
                                            onMoveToFolder = { onMoveTrackToFolder(track.id, it) }
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
                )
            }

            @Composable
            private fun ToolBar(
                modifier: Modifier = Modifier,
                currentFolder: StateFlow<Folder?>,
                path: List<Folder>,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit
            ) {
                val currentFolder by currentFolder.collectAsState()
                val pathLazyListState = rememberLazyListState()
                var newFolderFormVisible by remember { mutableStateOf(false) }

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
                            items(items = path, key = { it.id }) { folder ->
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
                            if (currentFolder == null) {
                                IconButton(
                                    onClick = onImportClick,
                                    content = { Icon(Icons.Default.ImportExport, null) }
                                )
                            }
                            IconButton(
                                onClick = { newFolderFormVisible = true },
                                content = { Icon(Icons.Default.CreateNewFolder, null) }
                            )
                        }
                    }
                }

                if (newFolderFormVisible) {
                    RenameDialog(
                        title = "New folder",
                        onDone = { onNewFolder(it); newFolderFormVisible = false },
                        onDismiss = { newFolderFormVisible = false }
                    )
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
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onPlayClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

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
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "Folder",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            minLines = 1,
                            maxLines = 1,
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
                        item = Item(
                            name = folder.name,
                            image = null // TODO
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
                            label = "Move to folder",
                            icon = Icons.Default.Folder,
                            onClick = { TODO() }
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

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename folder: ${folder.name}",
                        name = folder.name,
                        onDone = {
                            onRenameClick(it)
                            showEditFormDialog = false
                            showContextMenu = false
                        },
                        onDismiss = { showEditFormDialog = false }
                    )
                }

                if (showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        message = "Delete folder \"${folder.name}\" and all of its contents?",
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
            private fun PlaylistItem(
                modifier: Modifier = Modifier,
                playlist: Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        dev.younesgouyd.apps.music.common.components.util.compose.widgets.Image(
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
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "Playlist",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            minLines = 1,
                            maxLines = 1,
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
                            label = "Move to folder",
                            icon = Icons.Default.Folder,
                            onClick = { TODO() }
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

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename playlist: ${playlist.name}",
                        name = playlist.name,
                        onDone = {
                            onRenameClick(it)
                            showEditFormDialog = false
                            showContextMenu = false
                        },
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
            private fun TrackItem(
                modifier: Modifier = Modifier,
                track: Models.Track,
                onClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(modifier = modifier, onClick = onClick) {
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
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "Track",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(track.artists) { artist ->
                                    TextButton(
                                        content = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    modifier = Modifier.size(64.dp),
                                    data = track.image
                                )
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        options = {
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
                                label = "Move to folder",
                                icon = Icons.Default.Folder,
                                onClick = { TODO() }
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
                        },
                        onDismiss = { showContextMenu = false }
                    )
                }

                if (showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        message = "Delete track \"${track.name}\"?",
                        onDismissRequest = { showDeleteConfirmationDialog = false },
                        onYesClick = {
                            showDeleteConfirmationDialog = false
                            showContextMenu = false
                            onDeleteClick()
                        }
                    )
                }

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename track: ${track.name}",
                        name = track.name,
                        onDone = { onRenameClick(it); showEditFormDialog = false },
                        onDismiss = { showEditFormDialog = false }
                    )
                }
            }

            @Composable
            private fun RenameDialog(
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

        object Compact {
            @Composable
            fun Main(
                modifier: Modifier = Modifier,
                path: StateFlow<List<Models.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                currentFolder: StateFlow<Folder?>,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<Playlist>>,
                tracks: StateFlow<List<Models.Track>>,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onFolderClick: (Folder?) -> Unit,
                onAddFolderToPlaylistClick: (id: Long) -> Unit,
                onAddFolderToQueueClick: (id: Long) -> Unit,
                onPlayFolder: (id: Long) -> Unit,
                onPlaylistClick: (id: Long) -> Unit,
                onPlayPlaylistClick: (id: Long) -> Unit,
                onAddPlaylistToPlaylistClick: (id: Long) -> Unit,
                onAddPlaylistToQueueClick: (id: Long) -> Unit,
                onTrackClick: (id: Long) -> Unit,
                onAddTrackToPlaylistClick: (id: Long) -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onRenameFolder: (id: Long, name: String) -> Unit,
                onRenamePlaylist: (id: Long, name: String) -> Unit,
                onDeleteFolder: (id: Long) -> Unit,
                onDeletePlaylist: (id: Long) -> Unit,
                onDeleteTrack: (id: Long) -> Unit,
                onAddTrackToQueue: (id: Long) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit,
                onRenameTrack: (id: Long, name: String) -> Unit,
                onMoveFolderToFolder: (id: Long, folderId: Long) -> Unit,
                onMoveTrackToFolder: (id: Long, folderId: Long) -> Unit,
                onMovePlaylistToFolder: (id: Long, folderId: Long) -> Unit
            ) {
                val path by path.collectAsState()
                val loadingItems by loadingItems.collectAsState()
                val folders by folders.collectAsState()
                val playlists by playlists.collectAsState()
                val tracks by tracks.collectAsState()
                val scrollState = remember(path.last().folder?.id) {
                    path.last().scrollState
                }
                Scaffold(
                    modifier = modifier,
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) },
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.size(12.dp))
                            ToolBar(
                                modifier = Modifier.fillMaxWidth(),
                                currentFolder = currentFolder,
                                path = path.mapNotNull { it.folder },
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder
                            )
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(100.dp)
                            ) {
                                items(folders, { "folder#${it.id}"}) { folder ->
                                    FolderItem(
                                        folder = folder,
                                        onClick = { onFolderClick(folder) },
                                        onAddToPlaylistClick = { onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { onPlayFolder(folder.id) },
                                        onRenameClick = { onRenameFolder(folder.id, it) },
                                        onDeleteClick = { onDeleteFolder(folder.id) },
                                        onMoveToFolder = { onMoveFolderToFolder(folder.id, it) }
                                    )
                                }
                                items(playlists, { "playlist#${it.id}" }) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
                                        onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                        onAddToPlaylistClick = { onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { onDeletePlaylist(playlist.id) },
                                        onMoveToFolder = { onMovePlaylistToFolder(playlist.id, it) }
                                    )
                                }
                                items(tracks, { "track#${it.id}" }) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onDeleteClick = { onDeleteTrack(track.id) },
                                        onAddToQueueClick = { onAddTrackToQueue(track.id) },
                                        onRenameClick = { onRenameTrack(track.id, it) },
                                        onMoveToFolder = { onMoveTrackToFolder(track.id, it) }
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
                )
            }

            @Composable
            private fun ToolBar(
                modifier: Modifier = Modifier,
                currentFolder: StateFlow<Folder?>,
                path: List<Folder>,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
            ) {
                val currentFolder by currentFolder.collectAsState()
                val pathLazyListState = rememberLazyListState()
                var newFolderFormVisible by remember { mutableStateOf(false) }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                            items(items = path, key = { it.id }) { folder ->
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
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                if (currentFolder == null) {
                                    IconButton(
                                        onClick = onImportClick,
                                        content = { Icon(Icons.Default.ImportExport, null) }
                                    )
                                }
                                IconButton(
                                    onClick = { newFolderFormVisible = true },
                                    content = { Icon(Icons.Default.CreateNewFolder, null) }
                                )
                            }
                        }
                    }
                }

                if (newFolderFormVisible) {
                    RenameDialog(
                        title = "New folder",
                        onDone = { onNewFolder(it); newFolderFormVisible = false },
                        onDismiss = { newFolderFormVisible = false }
                    )
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
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onPlayClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

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
                            imageVector = Icons.Default.Folder,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "Folder",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = folder.name,
                            image = null // TODO
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
                            label = "Move to folder",
                            icon = Icons.Default.Folder,
                            onClick = { TODO() }
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

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename folder: ${folder.name}",
                        name = folder.name,
                        onDone = {
                            onRenameClick(it)
                            showEditFormDialog = false
                            showContextMenu = false
                        },
                        onDismiss = { showEditFormDialog = false }
                    )
                }

                if (showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        message = "Delete folder \"${folder.name}\" and all of its contents?",
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
            private fun PlaylistItem(
                modifier: Modifier = Modifier,
                playlist: Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

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
                        dev.younesgouyd.apps.music.common.components.util.compose.widgets.Image(
                            modifier = Modifier.aspectRatio(1f),
                            data = playlist.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "Playlist",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(name = playlist.name, image = playlist.image),
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
                            label = "Move to folder",
                            icon = Icons.Default.Folder,
                            onClick = { TODO() }
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

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename playlist: ${playlist.name}",
                        name = playlist.name,
                        onDone = {
                            onRenameClick(it)
                            showEditFormDialog = false
                            showContextMenu = false
                        },
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
            private fun TrackItem(
                modifier: Modifier = Modifier,
                track: Models.Track,
                onClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (id: Long) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onMoveToFolder: (id: Long) -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

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
                            data = track.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(track.artists) { artist ->
                                    TextButton(
                                        content = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Person, null)
                                                Text(
                                                    text = artist.name,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        },
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    modifier = Modifier.size(64.dp),
                                    data = track.image
                                )
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        options = {
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
                                label = "Move to folder",
                                icon = Icons.Default.Folder,
                                onClick = { TODO() }
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
                        },
                        onDismiss = { showContextMenu = false }
                    )
                }

                if (showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        message = "Delete track \"${track.name}\"?",
                        onDismissRequest = { showDeleteConfirmationDialog = false },
                        onYesClick = {
                            showDeleteConfirmationDialog = false
                            showContextMenu = false
                            onDeleteClick()
                        }
                    )
                }

                if (showEditFormDialog) {
                    RenameDialog(
                        title = "Rename track: ${track.name}",
                        name = track.name,
                        onDone = { onRenameClick(it); showEditFormDialog = false },
                        onDismiss = { showEditFormDialog = false }
                    )
                }
            }

            @Composable
            private fun RenameDialog(
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
}
