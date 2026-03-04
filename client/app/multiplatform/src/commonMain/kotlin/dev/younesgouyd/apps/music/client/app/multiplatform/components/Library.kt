package dev.younesgouyd.apps.music.client.app.multiplatform.components

import androidx.compose.foundation.Image
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
import dev.younesgouyd.apps.music.client.app.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.app.multiplatform.Music
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Folder
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Playlist
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.TrackRelation
import dev.younesgouyd.apps.music.client.app.multiplatform.usecases.ClearImportItemUseCase
import dev.younesgouyd.apps.music.client.app.multiplatform.usecases.DeleteFolderUseCase
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Library(
    private val tagRepo: TagRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val albumRepo: SpotifyAlbumRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: SpotifyArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val mediaFileRepo: MediaFileRepo,
    deleteFolderUseCase: DeleteFolderUseCase,
    clearImportItemUseCase: ClearImportItemUseCase,
    private val mediaController: MediaController,
    private val showImportFolderFlow: (FolderId) -> Unit,
    private val showImportFromInternetFlow: (FolderId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Library"
    private val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    private val selectedTags = MutableStateFlow(emptyList<TagId>())
    private val loadingFolders = MutableStateFlow(true)
    private val loadingPlaylists = MutableStateFlow(true)
    private val loadingTracks = MutableStateFlow(true)
    private val importingFolder = MutableStateFlow(false)
    private val deletingFolder = MutableStateFlow(false)
    private val addToPlaylistDialogVisible = MutableStateFlow(false)
    private val moveToFolderDialogVisible = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val moveToFolder: MutableStateFlow<MoveToFolder?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")
    private val tagSearchQuery = MutableStateFlow("")
    private val enableFiltering = MutableStateFlow(false)
    private val includeUntagged = MutableStateFlow(false)
    private val path: StateFlow<List<Ui.State.NodeState>>
    private val uiState: Ui.State

    private data class Filter(
        val currentFolder: Folder?,
        val search: String,
        val tags: List<TagId>,
        val enableFiltering: Boolean,
        val includeUntagged: Boolean
    )
    init {
        val root = Ui.State.NodeState(null, LazyGridState())
        var list: List<Ui.State.NodeState> = listOf(root)
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
                        temp.add(Ui.State.NodeState(folder, LazyGridState()))
                    }
                    list = temp
                    emit(list)
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = list)
        uiState = Ui.State(
            path = path,
            loadingItems = combine(
                loadingFolders, loadingPlaylists,
                loadingTracks, importingFolder
            ) { l1, l2, l3, l4 -> l1 || l2 || l3 || l4 }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true),
            searchQuery = searchQuery.asStateFlow(),
            tagsFilterState = TagsFilterState(
                tags = combine(tagSearchQuery, selectedTags) { search, selected -> Pair(search, selected) }
                    .flatMapLatest { (search, selected) ->
                        tagRepo.search(search).map {
                            it.sortedWith { first, second ->
                                val b1 = selected.contains(first.id)
                                val b2 = selected.contains(second.id)
                                if (b1 && b2) 0
                                else if (b1) -1
                                else 1
                            }.map { dbTag ->
                                TagsFilterState.Tag(
                                    id = dbTag.id,
                                    name = dbTag.name,
                                    selected = selected.contains(dbTag.id)
                                )
                            }
                        }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                searchQuery = tagSearchQuery.asStateFlow(),
                enableFiltering = enableFiltering.asStateFlow(),
                includeUntagged = includeUntagged.asStateFlow(),
                onSearchQueryChange = { tagSearchQuery.value = it },
                onEnableFilteringChange = { enableFiltering.value = it },
                onIncludeUntaggedChange = { includeUntagged.value = it },
                checkTag = { id ->
                    selectedTags.update { list ->
                        if (list.contains(id)) TODO()
                        list + listOf(id)
                    }
                },
                uncheckTag = { id ->
                    selectedTags.update { list ->
                        if (!list.contains(id)) TODO()
                        list.filter { it != id }
                    }
                }
            ),
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
            tracks = combine(
                currentFolder, searchQuery,
                selectedTags, enableFiltering,
                includeUntagged
            ) { currentFolder, searchQuery, selectedTags, enableFiltering, includeUntagged ->
                Filter(currentFolder, searchQuery, selectedTags, enableFiltering, includeUntagged)
            }.onEach { loadingTracks.value = true }
                .flatMapLatest { (currentFolder, searchQuery, selectedTags, enableFiltering, includeUntagged) ->
                    when {
                        currentFolder == null -> flow { emit(emptyList<TrackRelation>()) }
                        enableFiltering -> trackRepo.searchFolder(currentFolder.id, searchQuery, selectedTags, includeUntagged)
                        else -> trackRepo.searchFolder(currentFolder.id, searchQuery)
                    }
                }.mapLatest { dbTracks -> dbTracks.toTrackModels() }
                .onEach { loadingTracks.value = false }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            onNewFolder = ::addFolder,
            onFolderClick = { currentFolder.value = it },
            onSearchQueryChange = { value: String -> searchQuery.value = value },
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
                        playlistRepo = playlistRepo,
                        albumRepo = albumRepo
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
                        playlistRepo = playlistRepo,
                        albumRepo = albumRepo
                    )
                }
                addToPlaylistDialogVisible.value = true
            },
            onMoveFolderToFolder = { id: FolderId ->
                moveToFolder.update {
                    MoveToFolder(
                        itemToMove = MoveToFolder.ItemToMove.Folder(id),
                        folderRepo = folderRepo,
                        trackRepo = trackRepo,
                        mediaFileRepo = mediaFileRepo,
                        playlistRepo = playlistRepo,
                        dismiss = ::dismissMoveToFolderDialog
                    )
                }
                moveToFolderDialogVisible.value = true
            },
            onMoveTrackToFolder = { id: TrackId ->
                moveToFolder.update {
                    MoveToFolder(
                        itemToMove = MoveToFolder.ItemToMove.Track(id),
                        folderRepo = folderRepo,
                        trackRepo = trackRepo,
                        mediaFileRepo = mediaFileRepo,
                        playlistRepo = playlistRepo,
                        dismiss = ::dismissMoveToFolderDialog
                    )
                }
                moveToFolderDialogVisible.value = true
            },
            onMovePlaylistToFolder = { id: PlaylistId ->
                moveToFolder.update {
                    MoveToFolder(
                        itemToMove = MoveToFolder.ItemToMove.Playlist(id),
                        folderRepo = folderRepo,
                        trackRepo = trackRepo,
                        mediaFileRepo = mediaFileRepo,
                        playlistRepo = playlistRepo,
                        dismiss = ::dismissMoveToFolderDialog
                    )
                }
                moveToFolderDialogVisible.value = true
            },
            onAddPlaylistToQueueClick = { id: PlaylistId ->
                mediaController.addToQueue(
                    listOf(MediaController.QueueItemParameter.Playlist(id))
                )
            },
            onTrackClick = showTrack,
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
            onDeleteFolder = { folderId ->
                coroutineScope.launch {
                    deletingFolder.value = true
                    deleteFolderUseCase.execute(folderId)
                    deletingFolder.value = false
                }
            },
            onDeletePlaylist = { coroutineScope.launch { playlistRepo.delete(it) } },
            onDeleteTrack = { coroutineScope.launch { clearImportItemUseCase.execute(it) } },
            onAddTrackToQueue = { mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
            onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        val addToPlaylistDialogVisible by addToPlaylistDialogVisible.collectAsState()
        val moveToFolderDialogVisible by moveToFolderDialogVisible.collectAsState()
        val deletingFolder by deletingFolder.collectAsState()
        var isImportTypeDialogVisible by remember { mutableStateOf(false) }
        val addToPlaylist by addToPlaylist.collectAsState()
        val moveToFolder by moveToFolder.collectAsState()
        val path by path.collectAsState()

        DisposableEffect(Unit) {
            Music.registerBackHandler {
                if (path.size > 1) {
                    currentFolder.value = path.getOrNull(path.size - 2)?.folder
                }
            }
            onDispose {
                Music.unregisterLastBackHandler()
            }
        }

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
                onImportFolderClick = {
                    isImportTypeDialogVisible = false
                    showImportFolderFlow(currentFolder.value!!.id)
                },
                onImportFromInternetClick = {
                    isImportTypeDialogVisible = false
                    showImportFromInternetFlow(currentFolder.value!!.id)
                },
                onDismiss = { isImportTypeDialogVisible = false }
            )
        }
        if (addToPlaylistDialogVisible) {
            Dialog(onDismissRequest = ::dismissAddToPlaylistDialog) {
                addToPlaylist!!.show(Modifier)
            }
        }
        if (moveToFolderDialogVisible) {
            Dialog(onDismissRequest = ::dismissMoveToFolderDialog) {
                moveToFolder!!.show(Modifier)
            }
        }
        if (deletingFolder) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier.size(400.dp, 200.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text("Please wait while removing the folder...")
                    }
                }
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun List<TrackRelation>.toTrackModels(): List<Ui.State.Track> {
        return this.map { dbTrack ->
            Ui.State.Track(
                id = dbTrack.track.id,
                importSessionItemId = dbTrack.track.importSessionItemId,
                name = dbTrack.spotifyTrack?.name ?: dbTrack.originalImport.title,
                image = if (dbTrack.spotifyTrack != null) {
                    mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack.spotifyAlbumId)
                } else {
                    mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
                },
                artists = if (dbTrack.spotifyTrack != null) {
                    artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack.id).first().map { dbArtist ->
                        Ui.State.Track.Artist(dbArtist.id, dbArtist.name)
                    }
                } else {
                    dbTrack.originalImport.inspection.artists.map {
                        Ui.State.Track.Artist(null, it)
                    }
                }
            )
        }
    }

    private fun List<Playlist>.toPlaylistModels(): List<Ui.State.Playlist> {
        return this.map { dbPlaylist ->
            Ui.State.Playlist(
                id = dbPlaylist.id,
                name = dbPlaylist.name
            )
        }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it!!.clear(); null }
    }

    private fun dismissMoveToFolderDialog() {
        if (moveToFolder.value?.moving?.value == true) {
            return
        }
        moveToFolderDialogVisible.update { false }
        moveToFolder.update { it!!.clear(); null }
    }

    private fun addFolder(name: String) {
        coroutineScope.launch {
            folderRepo.add(
                name = name,
                parentFolderId = currentFolder.value?.id
            )
        }
    }

    private object Ui {
        data class State(
            val path: StateFlow<List<NodeState>>,
            val loadingItems: StateFlow<Boolean>,
            val searchQuery: StateFlow<String>,
            val tagsFilterState: TagsFilterState,
            val onSearchQueryChange: (String) -> Unit,
            val folders: StateFlow<List<Folder>>,
            val playlists: StateFlow<List<Playlist>>,
            val tracks: StateFlow<List<Track>>,
            val onNewFolder: (name: String) -> Unit,
            val onFolderClick: (Folder?) -> Unit,
            val onAddFolderToPlaylistClick: (FolderId) -> Unit,
            val onAddFolderToQueueClick: (FolderId) -> Unit,
            val onPlayFolder: (FolderId) -> Unit,
            val onPlaylistClick: (PlaylistId) -> Unit,
            val onPlayPlaylistClick: (PlaylistId) -> Unit,
            val onAddPlaylistToPlaylistClick: (PlaylistId) -> Unit,
            val onAddPlaylistToQueueClick: (PlaylistId) -> Unit,
            val onMoveTrackToFolder: (id: TrackId) -> Unit,
            val onMoveFolderToFolder: (id: FolderId) -> Unit,
            val onMovePlaylistToFolder: (id: PlaylistId) -> Unit,
            val onTrackClick: (TrackId) -> Unit,
            val onAddTrackToPlaylistClick: (TrackId) -> Unit,
            val onArtistClick: (SpotifyArtistId) -> Unit,
            val onRenameFolder: (id: FolderId, name: String) -> Unit,
            val onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
            val onDeleteFolder: (FolderId) -> Unit,
            val onDeletePlaylist: (PlaylistId) -> Unit,
            val onDeleteTrack: (ImportSessionItemId) -> Unit,
            val onAddTrackToQueue: (TrackId) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) {
            data class NodeState(
                val folder: Folder?,
                val scrollState: LazyGridState,
            )

            data class Track(
                val id: TrackId,
                val importSessionItemId: ImportSessionItemId,
                val name: String,
                val image: File?,
                val artists: List<Artist>
            ) {
                data class Artist(
                    val id: SpotifyArtistId?,
                    val name: String
                )
            }

            data class Playlist(
                val id: PlaylistId,
                val name: String
            )
        }

        object Common {
            @Composable
            fun ImportFormDialog(
                onImportFolderClick: () -> Unit,
                onImportFromInternetClick: () -> Unit,
                onDismiss: () -> Unit
            ) {
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
                            Text(
                                text = "Select source type",
                                style = MaterialTheme.typography.titleMedium
                            )
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
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onImportFolderClick,
                                        content = { Text("Import Folder") }
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onImportFromInternetClick,
                                        content = { Text("Import From Internet") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        object Wide {
            @Composable
            fun Main(
                modifier: Modifier,
                state: State,
                onImportClick: () -> Unit
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    loadingItems = state.loadingItems,
                    searchQuery = state.searchQuery,
                    tagsFilterState = state.tagsFilterState,
                    onSearchQueryChange = state.onSearchQueryChange,
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
                    onMoveFolderToFolder = state.onMoveFolderToFolder,
                    onMoveTrackToFolder = state.onMoveTrackToFolder,
                    onMovePlaylistToFolder = state.onMovePlaylistToFolder,
                    onAddPlaylistToQueueClick = state.onAddPlaylistToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onArtistClick = state.onArtistClick,
                    onRenameFolder = state.onRenameFolder,
                    onRenamePlaylist = state.onRenamePlaylist,
                    onDeleteFolder = state.onDeleteFolder,
                    onDeletePlaylist = state.onDeletePlaylist,
                    onDeleteTrack = state.onDeleteTrack,
                    onAddTrackToQueue = state.onAddTrackToQueue,
                    onDismissAddToPlaylistDialog = state.onDismissAddToPlaylistDialog
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                path: StateFlow<List<State.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                onSearchQueryChange: (String) -> Unit,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<State.Playlist>>,
                tracks: StateFlow<List<State.Track>>,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onFolderClick: (Folder?) -> Unit,
                onAddFolderToPlaylistClick: (FolderId) -> Unit,
                onAddFolderToQueueClick: (FolderId) -> Unit,
                onPlayFolder: (FolderId) -> Unit,
                onPlaylistClick: (PlaylistId) -> Unit,
                onPlayPlaylistClick: (PlaylistId) -> Unit,
                onAddPlaylistToPlaylistClick: (PlaylistId) -> Unit,
                onMoveFolderToFolder: (id: FolderId) -> Unit,
                onMoveTrackToFolder: (id: TrackId) -> Unit,
                onMovePlaylistToFolder: (id: PlaylistId) -> Unit,
                onAddPlaylistToQueueClick: (PlaylistId) -> Unit,
                onTrackClick: (TrackId) -> Unit,
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onRenameFolder: (id: FolderId, name: String) -> Unit,
                onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
                onDeleteFolder: (FolderId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onDeleteTrack: (ImportSessionItemId) -> Unit,
                onAddTrackToQueue: (TrackId) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit
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
                                searchQuery = searchQuery,
                                tagsFilterState = tagsFilterState,
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder,
                                onSearchQueryChange = onSearchQueryChange
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
                                        onMoveToFolder = { onMoveFolderToFolder(folder.id) },
                                        onAddToPlaylistClick = { onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { onPlayFolder(folder.id) },
                                        onRenameClick = { onRenameFolder(folder.id, it) },
                                        onDeleteClick = { onDeleteFolder(folder.id) }
                                    )
                                }
                                items(playlists, { "playlist#${it.id}" }) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
                                        onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                        onMoveToFolder = { onMovePlaylistToFolder(playlist.id) },
                                        onAddToPlaylistClick = { onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { onDeletePlaylist(playlist.id) }
                                    )
                                }
                                items(tracks, { "track#${it.id}" }) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onMoveToFolder = { onMoveTrackToFolder(track.id) },
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onDeleteClick = { onDeleteTrack(track.importSessionItemId) },
                                        onAddToQueueClick = { onAddTrackToQueue(track.id) }
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
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit
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
                                if (path.isNotEmpty()) {
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
                    TagsFilter(
                        modifier = Modifier.fillMaxWidth(),
                        state = tagsFilterState
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
                folder: Folder,
                onClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onPlayClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(onClick = onClick) {
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
                            onClick = onMoveToFolder
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
                playlist: State.Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(onClick = onClick) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            file = null,
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
                            image = null
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
                            onClick = onMoveToFolder
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
                track: State.Track,
                onClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

                Item(onClick = onClick) {
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
                                        enabled = artist.id != null,
                                        onClick = { onArtistClick(artist.id!!) }
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
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = { showDeleteConfirmationDialog = true },
                            )
                            Option(
                                label = "Move to folder",
                                icon = Icons.Default.Folder,
                                onClick = onMoveToFolder
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
                state: State,
                onImportClick: () -> Unit
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    loadingItems = state.loadingItems,
                    searchQuery = state.searchQuery,
                    tagsFilterState = state.tagsFilterState,
                    onSearchQueryChange = state.onSearchQueryChange,
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
                    onMoveFolderToFolder = state.onMoveFolderToFolder,
                    onMoveTrackToFolder = state.onMoveTrackToFolder,
                    onMovePlaylistToFolder = state.onMovePlaylistToFolder,
                    onAddPlaylistToQueueClick = state.onAddPlaylistToQueueClick,
                    onTrackClick = state.onTrackClick,
                    onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                    onArtistClick = state.onArtistClick,
                    onRenameFolder = state.onRenameFolder,
                    onRenamePlaylist = state.onRenamePlaylist,
                    onDeleteFolder = state.onDeleteFolder,
                    onDeletePlaylist = state.onDeletePlaylist,
                    onDeleteTrack = state.onDeleteTrack,
                    onAddTrackToQueue = state.onAddTrackToQueue,
                    onDismissAddToPlaylistDialog = state.onDismissAddToPlaylistDialog
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                path: StateFlow<List<State.NodeState>>,
                loadingItems: StateFlow<Boolean>,
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                onSearchQueryChange: (String) -> Unit,
                folders: StateFlow<List<Folder>>,
                playlists: StateFlow<List<State.Playlist>>,
                tracks: StateFlow<List<State.Track>>,
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
                onAddTrackToPlaylistClick: (TrackId) -> Unit,
                onMoveFolderToFolder: (id: FolderId) -> Unit,
                onMoveTrackToFolder: (id: TrackId) -> Unit,
                onMovePlaylistToFolder: (id: PlaylistId) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onRenameFolder: (id: FolderId, name: String) -> Unit,
                onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,
                onDeleteFolder: (FolderId) -> Unit,
                onDeletePlaylist: (PlaylistId) -> Unit,
                onDeleteTrack: (ImportSessionItemId) -> Unit,
                onAddTrackToQueue: (TrackId) -> Unit,
                onDismissAddToPlaylistDialog: () -> Unit
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
                                searchQuery = searchQuery,
                                tagsFilterState = tagsFilterState,
                                onFolderClick = onFolderClick,
                                onImportClick = onImportClick,
                                onNewFolder = onNewFolder,
                                onSearchQueryChange = onSearchQueryChange
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
                                        onMoveToFolder = { onMoveFolderToFolder(folder.id) },
                                        onAddToPlaylistClick = { onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { onPlayFolder(folder.id) },
                                        onRenameClick = { onRenameFolder(folder.id, it) },
                                        onDeleteClick = { onDeleteFolder(folder.id) }
                                    )
                                }
                                items(playlists, { "playlist#${it.id}" }) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
                                        onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                        onMoveToFolder = { onMovePlaylistToFolder(playlist.id) },
                                        onAddToPlaylistClick = { onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { onDeletePlaylist(playlist.id) }
                                    )
                                }
                                items(tracks, { "track#${it.id}" }) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onMoveToFolder = { onMoveTrackToFolder(track.id) },
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onDeleteClick = { onDeleteTrack(track.importSessionItemId) },
                                        onAddToQueueClick = { onAddTrackToQueue(track.id) }
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
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit
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
                                if (path.isNotEmpty()) {
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
                    TagsFilter(
                        modifier = Modifier.fillMaxWidth(),
                        state = tagsFilterState
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
                folder: Folder,
                onClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onPlayClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(
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
                            onClick = onMoveToFolder
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
                playlist: State.Playlist,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                var showEditFormDialog by remember { mutableStateOf(false) }

                Item(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            file = null,
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
                            image = null
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
                            onClick = onMoveToFolder
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
                track: State.Track,
                onClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

                Item(
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
                                        enabled = artist.id != null,
                                        onClick = { onArtistClick(artist.id!!) }
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
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = { showDeleteConfirmationDialog = true },
                            )
                            Option(
                                label = "Move to folder",
                                icon = Icons.Default.Folder,
                                onClick = onMoveToFolder
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
