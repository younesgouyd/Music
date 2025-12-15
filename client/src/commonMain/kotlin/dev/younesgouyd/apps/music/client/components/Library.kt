package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import dev.younesgouyd.apps.music.client.Music
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.client.components.util.compose.SystemFolderPicker
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.*
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.*
import dev.younesgouyd.apps.music.client.scanFolder
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class Library(
    private val server: Server,
    private val tagRepo: TagRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: ArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val importSessionWithItemsRepo: ImportSessionWithItemsRepo,
    private val mediaFileRepo: MediaFileRepo,
    private val mediaFileImportSessionCrossRefRepo: MediaFileImportSessionCrossRefRepo,
    private val mediaFileImportSessionItemCrossRefRepo: MediaFileImportSessionItemCrossRefRepo,
    private val mediaController: MediaController,
    showPlaylist: (PlaylistId) -> Unit,
    showArtistDetails: (ArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Library"
    private val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    private val selectedTags = MutableStateFlow(emptyList<TagId>())
    private val loadingFolders: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val loadingPlaylists: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val loadingTracks: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val importingFolder: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylistDialogVisible = MutableStateFlow(false)
    private val inspectionDialogVisible = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val inspection: MutableStateFlow<Inspection?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")
    private val tagSearchQuery = MutableStateFlow("")
    private val uiState: UiState

    init {
        val root = UiState.NodeState(null, LazyGridState())
        var list: List<UiState.NodeState> = listOf(root)

        uiState = UiState(
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
                            temp.add(UiState.NodeState(folder, LazyGridState()))
                        }
                        list = temp
                        emit(list)
                    }
                }
            }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = list),
            loadingItems = combine(
                loadingFolders, loadingPlaylists,
                loadingTracks, importingFolder
            ) { l1, l2, l3, l4 -> l1 || l2 || l3 || l4 }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true),
            tags = combine(tagSearchQuery, selectedTags) { query, selected -> Pair(query, selected) }
                .flatMapLatest { (query, selected) ->
                    tagRepo.search(query).map { tags ->
                        tags.sortedWith { first, second ->
                            val b1 = selected.contains(first.id)
                            val b2 = selected.contains(second.id)
                            if (b1 && b2) 0
                            else if (b1) -1
                            else 1
                        }
                    }
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            selectedTags = selectedTags.asStateFlow(),
            searchQuery = searchQuery.asStateFlow(),
            tagSearchQuery = tagSearchQuery.asStateFlow(),
            folders = combine(currentFolder, searchQuery) { folder, search -> Pair(folder, search) }
                .onEach { loadingFolders.value = true }
                .flatMapLatest { (folder, search) -> folderRepo.searchFolder(folder?.id, search) }
                .onEach { loadingFolders.value = false }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            playlists = combine(currentFolder, searchQuery) { folder, search -> Pair(folder, search) }
                .onEach { loadingPlaylists.value = true }
                .flatMapLatest { (folder, search) -> playlistRepo.searchFolder(folder?.id, search) }
                .map { dbPlaylists -> dbPlaylists.toPlaylistModels() }
                .onEach { loadingPlaylists.value = false }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            tracks = combine(currentFolder, searchQuery, selectedTags) { folder, search, tags -> Triple(folder, search, tags) }
                .onEach { loadingTracks.value = true }
                .flatMapLatest { (folder, search, tags) -> trackRepo.searchFolder(folder?.id, search, tags, false) }
                .map { dbTracks -> dbTracks.toTrackModels() }
                .onEach { loadingTracks.value = false }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            onNewFolder = ::addFolder,
            onFolderClick = { currentFolder.value = it },
            onSearchQueryChange = { value: String ->
                searchQuery.value = value
            },
            onTagSearchQueryChange = { value: String ->
                tagSearchQuery.value = value
            },
            onTagClick = { id: TagId ->
                selectedTags.update { list ->
                    if (list.contains(id)) {
                        list.filter { it != id }
                    } else {
                        list.toMutableList().also { it.add(id) }
                    }
                }
            },
            onAddFolderToPlaylistClick = { folderId: FolderId ->
                addToPlaylist.update {
                    if (it != null) TODO()
                    AddToPlaylist(
                        itemToAdd = AddToPlaylist.Item.Folder(folderId),
                        playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                        trackRepo = trackRepo,
                        folderRepo = folderRepo,
                        artistRepo = artistRepo,
                        mediaFileRepo = mediaFileRepo,
                        dismiss = ::dismissAddToPlaylistDialog,
                        playlistRepo = playlistRepo
                    )
                }
                addToPlaylistDialogVisible.value = true
            },
            onAddFolderToQueueClick = { id: FolderId ->
                suspend fun getFolderItems(_id: FolderId): List<MediaController.QueueItemParameter> {
                    val tracks = trackRepo.getFolderTracks(_id).first()
                        .map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
                    val playlists = playlistRepo.getFolderPlaylists(_id).first()
                        .map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
                    return tracks + playlists + folderRepo.getSubfolders(_id).first().flatMap { getFolderItems(it.id) }
                }
                coroutineScope.launch {
                    val queue = getFolderItems(id)
                    mediaController.addToQueue(queue)
                }
            },
            onPlayFolder = { folderId: FolderId ->
                suspend fun getFolderItems(_folderId: FolderId): List<MediaController.QueueItemParameter> {
                    val tracks = trackRepo.getFolderTracks(_folderId).first()
                        .map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
                    val playlists = playlistRepo.getFolderPlaylists(_folderId).first()
                        .map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
                    return tracks + playlists + folderRepo.getSubfolders(_folderId).first().flatMap { getFolderItems(it.id) }
                }
                coroutineScope.launch {
                    val queue = getFolderItems(folderId)
                    mediaController.playQueue(queue)
                }
            },
            onPlaylistClick = showPlaylist,
            onPlayPlaylistClick = { id: PlaylistId ->
                mediaController.playQueue(
                    listOf(MediaController.QueueItemParameter.Playlist(id))
                )
            },
            onAddPlaylistToPlaylistClick = { playlistId: PlaylistId ->
                addToPlaylist.update {
                    AddToPlaylist(
                        itemToAdd = AddToPlaylist.Item.Playlist(playlistId),
                        playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                        trackRepo = trackRepo,
                        folderRepo = folderRepo,
                        artistRepo = artistRepo,
                        mediaFileRepo = mediaFileRepo,
                        dismiss = ::dismissAddToPlaylistDialog,
                        playlistRepo = playlistRepo
                    )
                }
                addToPlaylistDialogVisible.value = true
            },
            onAddPlaylistToQueueClick = { id: PlaylistId ->
                mediaController.addToQueue(
                    listOf(MediaController.QueueItemParameter.Playlist(id))
                )
            },
            onTrackClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
            onTrackDetailsClick = showTrack,
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
                        playlistRepo = playlistRepo
                    )
                }
                addToPlaylistDialogVisible.value = true
            },
            onArtistClick = showArtistDetails,
            onRenameFolder = { id: FolderId, name: String ->
                coroutineScope.launch {
                    folderRepo.updateName(
                        id = id,
                        name = name
                    )
                }
            },
            onRenamePlaylist = { id: PlaylistId, name: String ->
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
            onRenameTrack = { id: TrackId, name: String ->
                coroutineScope.launch {
                    trackRepo.updateName(
                        id = id,
                        name = name
                    )
                }
            },
            onMoveFolderToFolder = { id: FolderId, destination: FolderId ->
                coroutineScope.launch {
                    folderRepo.updateParentFolderId(
                        id = id,
                        parentFolderId = destination
                    )
                }
            },
            onMoveTrackToFolder = { id: TrackId, destination: FolderId ->
                coroutineScope.launch {
                    trackRepo.updateFolderId(
                        id = id,
                        folderId = destination
                    )
                }
            },
            onMovePlaylistToFolder = { id: PlaylistId, destination: FolderId ->
                coroutineScope.launch {
                    playlistRepo.updateFolderId(
                        id = id,
                        folderId = destination
                    )
                }
            }
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        val inspectionDialogVisible by inspectionDialogVisible.collectAsState()
        val addToPlaylistDialogVisible by addToPlaylistDialogVisible.collectAsState()
        var isImportTypeDialogVisible by remember { mutableStateOf(false) }
        var preparingImportDialogVisible by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val inspection by inspection.collectAsState()
        val addToPlaylist by addToPlaylist.collectAsState()

        AdaptiveUi(
            wide = {
                Ui.Wide.Main(
                    modifier = modifier,
                    state = uiState,
                    onImportClick = { isImportTypeDialogVisible = true }
                )
            },
            compact = {
                Ui.Compact.Main(
                    modifier = modifier,
                    state = uiState,
                    onImportClick = { isImportTypeDialogVisible = true }
                )
            }
        )

        if (isImportTypeDialogVisible) {
            Ui.Common.ImportFormDialog(
                onFolderPicked = {
                    coroutineScope.launch {
                        importFolder(it)
                        isImportTypeDialogVisible = false
                    }
                },
                onUrlEntered = {
                    isImportTypeDialogVisible = false
                    showInspectionDialog(it)
                },
                onDismiss = { isImportTypeDialogVisible = false }
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
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun List<Track>.toTrackModels(): List<UiState.Track> {
        return this.map { dbTrack ->
            UiState.Track(
                id = dbTrack.id,
                name = dbTrack.name,
                image = mediaFileRepo.getTrackImage(dbTrack.id),
                artists = artistRepo.getTrackArtists(dbTrack.id)
                    .first()
                    .map { UiState.Track.Artist(id = it.id, name = it.name) }
            )
        }
    }

    private suspend fun List<Playlist>.toPlaylistModels(): List<UiState.Playlist> {
        return this.map { dbPlaylist ->
            UiState.Playlist(
                id = dbPlaylist.id,
                name = dbPlaylist.name,
                image = mediaFileRepo.getPlaylistImage(dbPlaylist.id)
            )
        }
    }

    private fun importFolder(uri: String) {
        Music.coroutineScope.launch {
            val items = scanFolder(uri)
            val (_, itemsWithId) = importSessionWithItemsRepo.addLocalSession(
                inspection = dev.younesgouyd.apps.music.common.Inspection.Folder(
                    container = dev.younesgouyd.apps.music.common.Inspection.ContainerInspection.Folder(uri = uri),
                    items = items
                ),
                destinationFolderId = currentFolder.value?.id
            )
            for ((id, item) in itemsWithId) {
                item.albumImage?.let { albumImage ->
                    val mediaFileId = mediaFileRepo.add(
                        type = MediaFile.Type.Image,
                        fileName = null,
                        data = Base64.decode(albumImage)
                    )
                    mediaFileImportSessionItemCrossRefRepo.add(
                        mediaFileId = mediaFileId,
                        importSessionItemId = id
                    )
                }
            }
        }
    }

    private fun importUrl(
        url: String,
        inspection: dev.younesgouyd.apps.music.common.Inspection.Webpage,
        ytDlpInspection: String,
        selected: List<Long>
    ) {
        coroutineScope.launch {
            val (importSessionId, itemsWithId) = importSessionWithItemsRepo.addUrlSession(
                url = url,
                inspection = inspection,
                ytDlpInspection = ytDlpInspection,
                selected = selected,
                destinationFolderId = currentFolder.value?.id
            )
            inspection.container.thumbnail?.let { thumbnail ->
                val playlistImageId = mediaFileRepo.add(
                    type = MediaFile.Type.Image,
                    fileName = null,
                    data = Base64.decode(thumbnail)
                )
                mediaFileImportSessionCrossRefRepo.add(
                    mediaFileId = playlistImageId,
                    importSessionId = importSessionId
                )
            }
            for ((id, item) in itemsWithId) {
                item.thumbnail?.let { thumbnail ->
                    val mediaFileId = mediaFileRepo.add(
                        type = MediaFile.Type.Image,
                        fileName = null,
                        data = Base64.decode(thumbnail)
                    )
                    mediaFileImportSessionItemCrossRefRepo.add(
                        mediaFileId = mediaFileId,
                        importSessionItemId = id
                    )
                }
            }
        }
    }

    private fun showInspectionDialog(url: String) {
        inspection.update {
            if (it != null) TODO()
            Inspection(
                server = server,
                url = url,
                onDone = { inspection, ytDlpInspection: String, selected ->
                    importUrl(
                        url = url,
                        inspection = inspection,
                        ytDlpInspection = ytDlpInspection,
                        selected = selected
                    )
                    dismissInspectionDialog()
                }
            )
        }
        inspectionDialogVisible.value = true
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

    private fun addFolder(name: String) {
        coroutineScope.launch {
            folderRepo.add(
                name = name,
                parentFolderId = currentFolder.value?.id
            )
        }
    }

    private data class UiState(
        val path: StateFlow<List<UiState.NodeState>>,
        val loadingItems: StateFlow<Boolean>,
        val tags: StateFlow<List<Tag>>,
        val selectedTags: StateFlow<List<TagId>>,
        val searchQuery: StateFlow<String>,
        val tagSearchQuery: StateFlow<String>,
        val onSearchQueryChange: (String) -> Unit,
        val onTagSearchQueryChange: (String) -> Unit,
        val onTagClick: (TagId) -> Unit,
        val folders: StateFlow<List<Folder>>,
        val playlists: StateFlow<List<UiState.Playlist>>,
        val tracks: StateFlow<List<UiState.Track>>,
        val onNewFolder: (name: String) -> Unit,
        val onFolderClick: (Folder?) -> Unit,
        val onAddFolderToPlaylistClick: (FolderId) -> Unit,
        val onAddFolderToQueueClick: (FolderId) -> Unit,
        val onPlayFolder: (FolderId) -> Unit,
        val onPlaylistClick: (PlaylistId) -> Unit,
        val onPlayPlaylistClick: (PlaylistId) -> Unit,
        val onAddPlaylistToPlaylistClick: (PlaylistId) -> Unit,
        val onAddPlaylistToQueueClick: (PlaylistId) -> Unit,
        val onTrackClick: (TrackId) -> Unit,
        val onTrackDetailsClick: (TrackId) -> Unit,
        val onAddTrackToPlaylistClick: (TrackId) -> Unit,
        val onArtistClick: (ArtistId) -> Unit,
        val onRenameFolder: (id: FolderId, name: String) -> Unit,
        val onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
        val onDeleteFolder: (FolderId) -> Unit,
        val onDeletePlaylist: (PlaylistId) -> Unit,
        val onDeleteTrack: (TrackId) -> Unit,
        val onAddTrackToQueue: (TrackId) -> Unit,
        val onDismissAddToPlaylistDialog: () -> Unit,
        val onRenameTrack: (id: TrackId, name: String) -> Unit,
        val onMoveFolderToFolder: (id: FolderId, destination: FolderId) -> Unit,
        val onMoveTrackToFolder: (id: TrackId, destination: FolderId) -> Unit,
        val onMovePlaylistToFolder: (id: PlaylistId, destination: FolderId) -> Unit
    ) {
        data class NodeState(
            val folder: Folder?,
            val scrollState: LazyGridState,
        )

        data class Track(
            val id: TrackId,
            val name: String,
            val image: File?,
            val artists: List<Artist>
        ) {
            data class Artist(
                val id: ArtistId,
                val name: String
            )
        }

        data class Playlist(
            val id: PlaylistId,
            val name: String,
            val image: File?
        )
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

                Dialog(onDismissRequest = onDismiss) {
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
                    SystemFolderPicker(
                        onFolderChosen = {
                            showSystemFilePicker = false
                            onFolderPicked(it)
                        },
                        onCancelled = { showSystemFilePicker = false }
                    )
                }
            }

            @Composable
            fun Tags(
                modifier: Modifier,
                tags: StateFlow<List<Tag>>,
                selectedTags: StateFlow<List<TagId>>,
                tagSearchQuery: StateFlow<String>,
                onTagSearchQueryChange: (String) -> Unit,
                onTagClick: (TagId) -> Unit
            ) {
                val tags by tags.collectAsState()
                val selectedTags by selectedTags.collectAsState()
                val tagSearchQuery by tagSearchQuery.collectAsState()
                var isSearchTagVisible by remember { mutableStateOf(false) }

                FlowRow(
                    modifier = modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    maxLines = 1
                ) {
                    if (isSearchTagVisible) {
                        OutlinedTextField(
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search tags") },
                            value = tagSearchQuery,
                            maxLines = 1,
                            onValueChange = onTagSearchQueryChange,
                            trailingIcon = {
                                IconButton(
                                    onClick = { isSearchTagVisible = false },
                                    content = { Icon(Icons.Default.Close, null) }
                                )
                            }
                        )
                    } else {
                        IconButton(
                            onClick = { isSearchTagVisible = true },
                            content = { Icon(Icons.Default.Search, null) }
                        )
                    }
                    for (tag in tags) {
                        val selected = selectedTags.contains(tag.id)
                        FilterChip(
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            },
                            label = { Text(tag.name) },
                            selected = selected,
                            onClick = { onTagClick(tag.id) }
                        )
                    }
                }
            }
        }

        object Wide {
            @Composable
            fun Main(
                modifier: Modifier,
                state: UiState,
                onImportClick: () -> Unit
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    loadingItems = state.loadingItems,
                    tags = state.tags,
                    selectedTags = state.selectedTags,
                    searchQuery = state.searchQuery,
                    tagSearchQuery = state.tagSearchQuery,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onTagSearchQueryChange = state.onTagSearchQueryChange,
                    onTagClick = state.onTagClick,
                    folders = state.folders,
                    playlists = state.playlists,
                    tracks = state.tracks,
                    onImportClick = onImportClick,
                    onNewFolder = state.onNewFolder,
                    onFolderClick = state.onFolderClick,
                    onAddFolderToPlaylistClick = state.onAddFolderToPlaylistClick,
                    onAddFolderToQueueClick = state.onAddFolderToQueueClick,
                    onPlayFolder = state.onPlayFolder,
                    onPlaylistClick = state.onPlaylistClick,
                    onPlayPlaylistClick = state.onPlayPlaylistClick,
                    onAddPlaylistToPlaylistClick = state.onAddPlaylistToPlaylistClick,
                    onAddPlaylistToQueueClick = state.onAddPlaylistToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onTrackDetailsClick = state.onTrackDetailsClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onArtistClick = state.onArtistClick,
                    onRenameFolder = state.onRenameFolder,
                    onRenamePlaylist = state.onRenamePlaylist,
                    onDeleteFolder = state.onDeleteFolder,
                    onDeletePlaylist = state.onDeletePlaylist,
                    onDeleteTrack = state.onDeleteTrack,
                    onAddTrackToQueue = state.onAddTrackToQueue,
                    onDismissAddToPlaylistDialog = state.onDismissAddToPlaylistDialog,
                    onRenameTrack = state.onRenameTrack,
                    onMoveFolderToFolder = state.onMoveFolderToFolder,
                    onMoveTrackToFolder = state.onMoveTrackToFolder,
                    onMovePlaylistToFolder = state.onMovePlaylistToFolder
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                path: StateFlow<List<UiState.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                tags: StateFlow<List<Tag>>,
                selectedTags: StateFlow<List<TagId>>,
                searchQuery: StateFlow<String>,
                tagSearchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onTagSearchQueryChange: (String) -> Unit,
                onTagClick: (TagId) -> Unit,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<UiState.Playlist>>,
                tracks: StateFlow<List<UiState.Track>>,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onFolderClick: (Folder?) -> Unit,
                onAddFolderToPlaylistClick: (FolderId) -> Unit,
                onAddFolderToQueueClick: (FolderId) -> Unit,
                onPlayFolder: (FolderId) -> Unit,
                onPlaylistClick: (PlaylistId) -> Unit,
                onPlayPlaylistClick: (PlaylistId) -> Unit,
                onAddPlaylistToPlaylistClick: (PlaylistId) -> Unit,
                onAddPlaylistToQueueClick: (PlaylistId) -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onTrackDetailsClick: (TrackId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onRenameFolder: (id: FolderId, name: String) -> Unit,
                onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
                onDeleteFolder: (FolderId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onDeleteTrack: (TrackId) -> Unit,
                onAddTrackToQueue: (TrackId) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit,
                onRenameTrack: (id: TrackId, name: String) -> Unit,
                onMoveFolderToFolder: (id: FolderId, destination: FolderId) -> Unit,
                onMoveTrackToFolder: (id: TrackId, destination: FolderId) -> Unit,
                onMovePlaylistToFolder: (id: PlaylistId, destination: FolderId) -> Unit
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
                                path = path.mapNotNull { it.folder },
                                tags = tags,
                                selectedTags = selectedTags,
                                searchQuery = searchQuery,
                                tagSearchQuery = tagSearchQuery,
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder,
                                onSearchQueryChange = onSearchQueryChange,
                                onTagSearchQueryChange = onTagSearchQueryChange,
                                onTagClick = onTagClick
                            )
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                items(folders, { "folder#${it.id}" }) { folder ->
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
                                        onDetailsClick = { onTrackDetailsClick(track.id) },
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
                modifier: Modifier,
                path: List<Folder>,
                tags: StateFlow<List<Tag>>,
                selectedTags: StateFlow<List<TagId>>,
                searchQuery: StateFlow<String>,
                tagSearchQuery: StateFlow<String>,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit,
                onTagSearchQueryChange: (String) -> Unit,
                onTagClick: (TagId) -> Unit
            ) {
                val pathLazyListState = rememberLazyListState()
                val searchQuery by searchQuery.collectAsState()
                var newFolderFormVisible by remember { mutableStateOf(false) }
                var isSearchVisible by remember { mutableStateOf(false) }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSearchVisible) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
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
                                        items(items = path) { folder ->
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
                                    IconButton(
                                        onClick = { isSearchVisible = true },
                                        content = { Icon(Icons.Default.Search, null) }
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                label = { Text("Search") },
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isSearchVisible = false },
                                        content = { Icon(Icons.Default.Close, null) }
                                    )
                                }
                            )
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
                                    onClick = onImportClick,
                                    content = { Icon(Icons.Default.ImportExport, null) }
                                )
                                IconButton(
                                    onClick = { newFolderFormVisible = true },
                                    content = { Icon(Icons.Default.CreateNewFolder, null) }
                                )
                            }
                        }
                    }
                    Common.Tags(
                        modifier = Modifier.fillMaxWidth(),
                        tags = tags,
                        selectedTags = selectedTags,
                        tagSearchQuery = tagSearchQuery,
                        onTagSearchQueryChange = onTagSearchQueryChange,
                        onTagClick = onTagClick
                    )
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
                onMoveToFolder: (FolderId) -> Unit
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
                playlist: UiState.Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (FolderId) -> Unit
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
                            file = playlist.image,
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
                track: UiState.Track,
                onClick: () -> Unit,
                onDetailsClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onMoveToFolder: (FolderId) -> Unit
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
                            file = track.image,
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
                                    file = track.image
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
                                label = "Details",
                                icon = Icons.Default.Info,
                                onClick = onDetailsClick
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
                modifier: Modifier,
                state: UiState,
                onImportClick: () -> Unit
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    loadingItems = state.loadingItems,
                    tags = state.tags,
                    selectedTags = state.selectedTags,
                    searchQuery = state.searchQuery,
                    tagSearchQuery = state.tagSearchQuery,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onTagSearchQueryChange = state.onTagSearchQueryChange,
                    onTagClick = state.onTagClick,
                    folders = state.folders,
                    playlists = state.playlists,
                    tracks = state.tracks,
                    onImportClick = onImportClick,
                    onNewFolder = state.onNewFolder,
                    onFolderClick = state.onFolderClick,
                    onAddFolderToPlaylistClick = state.onAddFolderToPlaylistClick,
                    onAddFolderToQueueClick = state.onAddFolderToQueueClick,
                    onPlayFolder = state.onPlayFolder,
                    onPlaylistClick = state.onPlaylistClick,
                    onPlayPlaylistClick = state.onPlayPlaylistClick,
                    onAddPlaylistToPlaylistClick = state.onAddPlaylistToPlaylistClick,
                    onAddPlaylistToQueueClick = state.onAddPlaylistToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onTrackDetailsClick = state.onTrackDetailsClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onArtistClick = state.onArtistClick,
                    onRenameFolder = state.onRenameFolder,
                    onRenamePlaylist = state.onRenamePlaylist,
                    onDeleteFolder = state.onDeleteFolder,
                    onDeletePlaylist = state.onDeletePlaylist,
                    onDeleteTrack = state.onDeleteTrack,
                    onAddTrackToQueue = state.onAddTrackToQueue,
                    onDismissAddToPlaylistDialog = state.onDismissAddToPlaylistDialog,
                    onRenameTrack = state.onRenameTrack,
                    onMoveFolderToFolder = state.onMoveFolderToFolder,
                    onMoveTrackToFolder = state.onMoveTrackToFolder,
                    onMovePlaylistToFolder = state.onMovePlaylistToFolder
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier = Modifier,
                path: StateFlow<List<UiState.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                tags: StateFlow<List<Tag>>,
                selectedTags: StateFlow<List<TagId>>,
                searchQuery: StateFlow<String>,
                tagSearchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onTagSearchQueryChange: (String) -> Unit,
                onTagClick: (TagId) -> Unit,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<UiState.Playlist>>,
                tracks: StateFlow<List<UiState.Track>>,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onFolderClick: (Folder?) -> Unit,
                onAddFolderToPlaylistClick: (FolderId) -> Unit,
                onAddFolderToQueueClick: (FolderId) -> Unit,
                onPlayFolder: (FolderId) -> Unit,
                onPlaylistClick: (PlaylistId) -> Unit,
                onPlayPlaylistClick: (PlaylistId) -> Unit,
                onAddPlaylistToPlaylistClick: (PlaylistId) -> Unit,
                onAddPlaylistToQueueClick: (PlaylistId) -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onTrackDetailsClick: (TrackId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onRenameFolder: (id: FolderId, name: String) -> Unit,
                onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
                onDeleteFolder: (FolderId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onDeleteTrack: (TrackId) -> Unit,
                onAddTrackToQueue: (TrackId) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit,
                onRenameTrack: (id: TrackId, name: String) -> Unit,
                onMoveFolderToFolder: (id: FolderId, destination: FolderId) -> Unit,
                onMoveTrackToFolder: (id: TrackId, destination: FolderId) -> Unit,
                onMovePlaylistToFolder: (id: PlaylistId, destination: FolderId) -> Unit
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
                                path = path.mapNotNull { it.folder },
                                tags = tags,
                                selectedTags = selectedTags,
                                searchQuery = searchQuery,
                                tagSearchQuery = tagSearchQuery,
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder,
                                onSearchQueryChange = onSearchQueryChange,
                                onTagSearchQueryChange = onTagSearchQueryChange,
                                onTagClick = onTagClick
                            )
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(100.dp)
                            ) {
                                items(folders, { "folder#${it.id}" }) { folder ->
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
                                        onDetailsClick = { onTrackDetailsClick(track.id) },
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
                modifier: Modifier,
                path: List<Folder>,
                tags: StateFlow<List<Tag>>,
                selectedTags: StateFlow<List<TagId>>,
                searchQuery: StateFlow<String>,
                tagSearchQuery: StateFlow<String>,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit,
                onTagSearchQueryChange: (String) -> Unit,
                onTagClick: (TagId) -> Unit
            ) {
                val pathLazyListState = rememberLazyListState()
                val searchQuery by searchQuery.collectAsState()
                var newFolderFormVisible by remember { mutableStateOf(false) }
                var isSearchVisible by remember { mutableStateOf(false) }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isSearchVisible) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
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
                                    items(items = path) { folder ->
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
                                IconButton(
                                    onClick = { isSearchVisible = true },
                                    content = { Icon(Icons.Default.Search, null) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search") },
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            trailingIcon = {
                                IconButton(
                                    onClick = { isSearchVisible = false },
                                    content = { Icon(Icons.Default.Close, null) }
                                )
                            }
                        )
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
                                IconButton(
                                    onClick = onImportClick,
                                    content = { Icon(Icons.Default.ImportExport, null) }
                                )
                                IconButton(
                                    onClick = { newFolderFormVisible = true },
                                    content = { Icon(Icons.Default.CreateNewFolder, null) }
                                )
                            }
                        }
                    }
                    Common.Tags(
                        modifier = Modifier.fillMaxWidth(),
                        tags = tags,
                        selectedTags = selectedTags,
                        tagSearchQuery = tagSearchQuery,
                        onTagSearchQueryChange = onTagSearchQueryChange,
                        onTagClick = onTagClick
                    )
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
                onMoveToFolder: (FolderId) -> Unit
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
                playlist: UiState.Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit,
                onMoveToFolder: (FolderId) -> Unit
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            file = playlist.image,
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
                track: UiState.Track,
                onClick: () -> Unit,
                onDetailsClick: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (ArtistId) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onMoveToFolder: (FolderId) -> Unit
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            file = track.image,
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
                                    file = track.image
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
                                label = "Details",
                                icon = Icons.Default.Info,
                                onClick = onDetailsClick
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
