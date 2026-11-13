package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
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
import dev.younesgouyd.apps.music.client.multiplatform.Application
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.client.multiplatform.usecases.ClearImportItemUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.DeleteFolderUseCase
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.Folder
import dev.younesgouyd.apps.music.common.models.datamodels.Playlist
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class Library(
    private val tagRepo: TagRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val albumRepo: SpotifyAlbumRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: SpotifyArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    deleteFolderUseCase: DeleteFolderUseCase,
    clearImportItemUseCase: ClearImportItemUseCase,
    private val mediaController: MediaController,
    private val showImportFlow: (FolderId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Library"
    private val path = MutableStateFlow(listOf(Ui.State.NodeState(null)))
    private val currentFolder: StateFlow<Folder?> = path.mapLatest { it.last().folder }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    private val selectedTags = MutableStateFlow(emptyList<TagId>())
    private val loadingFolders = MutableStateFlow(true)
    private val loadingPlaylists = MutableStateFlow(true)
    private val loadingTracks = MutableStateFlow(true)
    private val deletingFolder = MutableStateFlow(false)
    private val addToPlaylistDialogVisible = MutableStateFlow(false)
    private val moveToFolderDialogVisible = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val moveToFolder: MutableStateFlow<MoveToFolder?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow("")
    private val tagSearchQuery = MutableStateFlow("")
    private val enableFiltering = MutableStateFlow(false)
    private val includeUntagged = MutableStateFlow(false)
    private val uiState: Ui.State

    private data class TracksFilter(
        val currentFolder: Folder?,
        val search: String,
        val tags: List<TagId>,
        val enableFiltering: Boolean,
        val includeUntagged: Boolean
    )

    init {
        uiState = Ui.State(
            path = path,
            searchQuery = searchQuery.asStateFlow(),
            tagsFilterState = TagsFilterState(
                tags = combine(tagSearchQuery, selectedTags, ::Pair)
                    .flatMapLatest { (search, selected) ->
                        tagRepo.search(search).map {
                            it.sortedByDescending { selected.contains(it.id) }
                                .map { dbTag ->
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
            content = run {
                val folders = combine(currentFolder, searchQuery, ::Pair)
                    .onEach { loadingFolders.value = true }
                    .flatMapLatest { (folder, search) -> folderRepo.searchFolder(folder?.id, search) }
                    .onEach { loadingFolders.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val playlists: StateFlow<List<Ui.State.Content.Loaded.Playlist>> = combine(currentFolder, searchQuery, ::Pair)
                    .onEach { loadingPlaylists.value = true }
                    .flatMapLatest { (folder, search) -> playlistRepo.searchFolder(folder?.id, search) }
                    .map { dbPlaylists -> dbPlaylists.toPlaylistModels() }
                    .onEach { loadingPlaylists.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val tracks: StateFlow<List<Ui.State.Content.Loaded.Track>> = combine(
                    currentFolder, searchQuery,
                    selectedTags, enableFiltering,
                    includeUntagged, ::TracksFilter
                ).onEach { loadingTracks.value = true }
                    .flatMapLatest { (currentFolder, searchQuery, selectedTags, enableFiltering, includeUntagged) ->
                        when {
                            currentFolder == null -> flow { emit(emptyList()) }
                            enableFiltering -> trackRepo.searchFolderWithTags(currentFolder.id, searchQuery, selectedTags, includeUntagged)
                            else -> trackRepo.searchFolder(currentFolder.id, searchQuery)
                        }
                    }.mapLatest { dbTracks -> dbTracks.toTrackModels() }
                    .onEach { loadingTracks.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks) { l1, l2, l3 -> l1 || l2 || l3 }
                combine(
                    loadingItems, currentFolder.map { it?.id }, folders, playlists, tracks
                ) { loadingItems, currentFolder, folders, playlists, tracks ->
                    if (loadingItems) {
                        Ui.State.Content.Loading
                    } else {
                        Ui.State.Content.Loaded(
                            folderId = currentFolder,
                            folders = folders,
                            playlists = playlists,
                            tracks = tracks
                        )
                    }
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Content.Loading)
            },
            actions = Ui.State.Actions(
                onImportClick = { showImportFlow(path.value.last().folder!!.id) },
                onNewFolder = {
                    coroutineScope.launch {
                        folderRepo.add(name = it, parentFolderId = currentFolder.value?.id)
                    }
                },
                onFolderClick = ::openFolder,
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
                            dismiss = ::dismissAddToPlaylistDialog,
                            playlistRepo = playlistRepo,
                            albumRepo = albumRepo
                        )
                    }
                    addToPlaylistDialogVisible.value = true
                },
                onAddFolderToQueueClick = { id: FolderId ->
                    mediaController.addToQueue(
                        listOf(QueueItemParameter.Folder(id))
                    )
                },
                onPlayFolder = { id: FolderId ->
                    mediaController.playQueue(
                        listOf(QueueItemParameter.Folder(id))
                    )
                },
                onPlaylistClick = showPlaylist,
                onPlayPlaylistClick = { id: PlaylistId ->
                    mediaController.playQueue(
                        listOf(QueueItemParameter.Playlist(id))
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
                            playlistRepo = playlistRepo,
                            dismiss = ::dismissMoveToFolderDialog
                        )
                    }
                    moveToFolderDialogVisible.value = true
                },
                onAddPlaylistToQueueClick = { id: PlaylistId ->
                    mediaController.addToQueue(
                        listOf(QueueItemParameter.Playlist(id))
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
                onAddTrackToQueue = { mediaController.addToQueue(listOf(QueueItemParameter.Track(it))) },
                onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
            )
        )
    }

    private fun openFolder(folder: Folder?) {
        path.update { list ->
            if (folder == null) return@update list.take(1)
            val i = list.indexOfFirst { it.folder?.id == folder.id }
            if (i >= 0) list.take(i + 1) else list + Ui.State.NodeState(folder)
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val addToPlaylistDialogVisible by addToPlaylistDialogVisible.collectAsState()
        val moveToFolderDialogVisible by moveToFolderDialogVisible.collectAsState()
        val deletingFolder by deletingFolder.collectAsState()
        val addToPlaylist: Component? by addToPlaylist.collectAsState()
        val moveToFolder : Component? by moveToFolder.collectAsState()

        DisposableEffect(Unit) {
            Application.registerBackHandler {
                val path = path.value
                if (path.size > 1) {
                    openFolder(path[path.size - 2].folder)
                }
            }
            onDispose {
                Application.unregisterLastBackHandler()
            }
        }

        AdaptiveUi(
            wide = {
                Ui.Wide.Main(
                    modifier = modifier,
                    state = uiState
                )
            },
            compact = {
                Ui.Compact.Main(
                    modifier = modifier,
                    state = uiState
                )
            }
        )

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

    private fun List<TrackRelation>.toTrackModels(): List<Ui.State.Content.Loaded.Track> {
        return this.map { dbTrack ->
            Ui.State.Content.Loaded.Track(
                id = dbTrack.track.id,
                importSessionItemId = dbTrack.track.importSessionItemId,
                name = dbTrack.spotifyTrackRelation?.spotifyTrack?.name ?: dbTrack.originalImport.title,
                image = dbTrack.spotifyTrackRelation?.spotifyAlbum?.mediumImgId ?: dbTrack.originalImport.imgId,
                artists = run {
                    dbTrack.spotifyTrackRelation?.spotifyArtists?.map {
                        Ui.State.Content.Loaded.Track.Artist(it.id, it.name)
                    } ?: dbTrack.originalImport.inspection.artists.map {
                        Ui.State.Content.Loaded.Track.Artist(null, it)
                    }
                }
            )
        }
    }

    private fun List<Playlist>.toPlaylistModels(): List<Ui.State.Content.Loaded.Playlist> {
        return this.map { dbPlaylist ->
            Ui.State.Content.Loaded.Playlist(
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

    private object Ui {
        class State(
            val path: StateFlow<List<NodeState>>,
            val searchQuery: StateFlow<String>,
            val tagsFilterState: TagsFilterState,
            val content: StateFlow<Content>,
            val actions: Actions
        ) {
            class NodeState(
                val folder: Folder?
            ) {
                var firstVisibleItemIndex: Int = 0
                var firstVisibleItemScrollOffset: Int = 0
            }

            sealed class Content {
                data object Loading : Content()

                data class Loaded(
                    val folderId: FolderId?,
                    val folders: List<Folder>,
                    val playlists: List<Playlist>,
                    val tracks: List<Track>
                ) : Content() {
                    data class Track(
                        val id: TrackId,
                        val importSessionItemId: ImportSessionItemId,
                        val name: String,
                        val image: MediaFileId?,
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
            }

            data class Actions(
                val onImportClick: () -> Unit,
                val onSearchQueryChange: (String) -> Unit,
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
            )
        }

        @Composable
        fun Content(
            content: State.Content,
            gridState: LazyGridState,
            actions: State.Actions,
            folderUi: @Composable (Modifier, Folder, State.Actions) -> Unit,
            playlistUi: @Composable (Modifier, State.Content.Loaded.Playlist, State.Actions) -> Unit,
            trackUi: @Composable (Modifier, State.Content.Loaded.Track, State.Actions) -> Unit
        ) {
            val minColumnWidth: Dp = when (getWindowSizeClass()) {
                WindowWidthSizeClass.Compact -> 100.dp
                else -> 200.dp
            }

            when (content) {
                is State.Content.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(10.dp), Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is State.Content.Loaded -> {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        columns = GridCells.Adaptive(minColumnWidth)
                    ) {
                        val ratio = 0.65f
                        items(content.folders, { "folder#${it.id}" }) { folder -> folderUi(Modifier.aspectRatio(ratio), folder, actions) }
                        items(content.playlists, { "playlist#${it.id}" }) { playlist -> playlistUi(Modifier.aspectRatio(ratio), playlist, actions) }
                        items(content.tracks, { "track#${it.id}" }) { track -> trackUi(Modifier.aspectRatio(ratio), track, actions) }
                    }
                }
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

        object Wide {
            @Composable
            fun Main(
                modifier: Modifier,
                state: State
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    searchQuery = state.searchQuery,
                    tagsFilterState = state.tagsFilterState,
                    content = state.content,
                    actions = state.actions
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                path: StateFlow<List<State.NodeState>>,
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                content: StateFlow<State.Content>,
                actions: State.Actions
            ) {
                val path by path.collectAsState()
                val content by content.collectAsState()
                val node = path.last()
                val gridState = remember(node) {
                    LazyGridState(node.firstVisibleItemIndex, node.firstVisibleItemScrollOffset)
                }

                DisposableEffect(node) {
                    onDispose {
                        node.firstVisibleItemIndex = gridState.firstVisibleItemIndex
                        node.firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
                    }
                }

                Scaffold(
                    modifier = modifier,
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ToolBar(
                                modifier = Modifier.fillMaxWidth(),
                                path = path.mapNotNull { it.folder },
                                searchQuery = searchQuery,
                                tagsFilterState = tagsFilterState,
                                onFolderClick = actions.onFolderClick,
                                onImportClick = actions.onImportClick,
                                onNewFolder = actions.onNewFolder,
                                onSearchQueryChange = actions.onSearchQueryChange
                            )
                            Content(
                                content = content,
                                gridState = gridState,
                                actions = actions,
                                folderUi = { modifier, folder, actions ->
                                    FolderItem(
                                        modifier = modifier,
                                        folder = folder,
                                        onClick = { actions.onFolderClick(folder) },
                                        onMoveToFolder = { actions.onMoveFolderToFolder(folder.id) },
                                        onAddToPlaylistClick = { actions.onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { actions.onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { actions.onPlayFolder(folder.id) },
                                        onRenameClick = { actions.onRenameFolder(folder.id, it) },
                                        onDeleteClick = { actions.onDeleteFolder(folder.id) }
                                    )

                                },
                                playlistUi = { modifier, playlist, actions ->
                                    PlaylistItem(
                                        modifier = modifier,
                                        playlist = playlist,
                                        onClick = { actions.onPlaylistClick(playlist.id) },
                                        onPlayClick = { actions.onPlayPlaylistClick(playlist.id) },
                                        onMoveToFolder = { actions.onMovePlaylistToFolder(playlist.id) },
                                        onAddToPlaylistClick = { actions.onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { actions.onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { actions.onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { actions.onDeletePlaylist(playlist.id) }
                                    )

                                },
                                trackUi = { modifier, track, actions ->
                                    TrackItem(
                                        modifier = modifier,
                                        track = track,
                                        onClick = { actions.onTrackClick(track.id) },
                                        onMoveToFolder = { actions.onMoveTrackToFolder(track.id) },
                                        onAddToPlaylistClick = { actions.onAddTrackToPlaylistClick(track.id) },
                                        onArtistClick = actions.onArtistClick,
                                        onDeleteClick = { actions.onDeleteTrack(track.importSessionItemId) },
                                        onAddToQueueClick = { actions.onAddTrackToQueue(track.id) }
                                    )
                                }
                            )
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(gridState) }
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
                                        onClick = { isSearchVisible = false; onSearchQueryChange("") },
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
                modifier: Modifier,
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
                            imageVector = Icons.Default.Folder,
                            contentScale = ContentScale.Fit,
                            contentDescription = null
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = folder.name,
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
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
                modifier: Modifier,
                playlist: State.Content.Loaded.Playlist,
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
                            id = null,
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
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
                modifier: Modifier,
                track: State.Content.Loaded.Track,
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
                            id = track.image,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = track.name,
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
                                                    style = MaterialTheme.typography.labelSmall,
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
                                    id = track.image
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
//                            Option(
//                                label = "Play next",
//                                icon = Icons.Default.QueuePlayNext,
//                                onClick = { TODO() },
//                            )
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
        }

        object Compact {
            @Composable
            fun Main(
                modifier: Modifier,
                state: State
            ) {
                Main(
                    modifier = modifier,
                    path = state.path,
                    searchQuery = state.searchQuery,
                    content = state.content,
                    actions = state.actions
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                path: StateFlow<List<State.NodeState>>,
                searchQuery: StateFlow<String>,
                content: StateFlow<State.Content>,
                actions: State.Actions
            ) {
                val path by path.collectAsState()
                val content by content.collectAsState()
                val node = path.last()
                val gridState = remember(node) {
                    LazyGridState(node.firstVisibleItemIndex, node.firstVisibleItemScrollOffset)
                }

                DisposableEffect(node) {
                    onDispose {
                        node.firstVisibleItemIndex = gridState.firstVisibleItemIndex
                        node.firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
                    }
                }

                Scaffold(
                    modifier = modifier,
                    floatingActionButton = { ScrollToTopFloatingActionButton(gridState) },
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ToolBar(
                                modifier = Modifier.fillMaxWidth(),
                                path = path.mapNotNull { it.folder },
                                searchQuery = searchQuery,
                                onFolderClick = actions.onFolderClick,
                                onImportClick = actions.onImportClick,
                                onNewFolder = actions.onNewFolder,
                                onSearchQueryChange = actions.onSearchQueryChange
                            )
                            Content(
                                content = content,
                                gridState = gridState,
                                actions = actions,
                                folderUi = { modifier, folder, actions ->
                                    FolderItem(
                                        modifier = modifier,
                                        folder = folder,
                                        onClick = { actions.onFolderClick(folder) },
                                        onMoveToFolder = { actions.onMoveFolderToFolder(folder.id) },
                                        onAddToPlaylistClick = { actions.onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { actions.onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { actions.onPlayFolder(folder.id) },
                                        onRenameClick = { actions.onRenameFolder(folder.id, it) },
                                        onDeleteClick = { actions.onDeleteFolder(folder.id) }
                                    )

                                },
                                playlistUi = { modifier, playlist, actions ->
                                    PlaylistItem(
                                        modifier = modifier,
                                        playlist = playlist,
                                        onClick = { actions.onPlaylistClick(playlist.id) },
                                        onPlayClick = { actions.onPlayPlaylistClick(playlist.id) },
                                        onMoveToFolder = { actions.onMovePlaylistToFolder(playlist.id) },
                                        onAddToPlaylistClick = { actions.onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { actions.onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { actions.onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { actions.onDeletePlaylist(playlist.id) }
                                    )

                                },
                                trackUi = { modifier, track, actions ->
                                    TrackItem(
                                        modifier = modifier,
                                        track = track,
                                        onClick = { actions.onTrackClick(track.id) },
                                        onMoveToFolder = { actions.onMoveTrackToFolder(track.id) },
                                        onAddToPlaylistClick = { actions.onAddTrackToPlaylistClick(track.id) },
                                        onDeleteClick = { actions.onDeleteTrack(track.importSessionItemId) },
                                        onAddToQueueClick = { actions.onAddTrackToQueue(track.id) }
                                    )
                                }
                            )
                        }
                    }
                )
            }

            @Composable
            private fun ToolBar(
                modifier: Modifier,
                path: List<Folder>,
                searchQuery: StateFlow<String>,
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
                                    onClick = { isSearchVisible = false; onSearchQueryChange("") },
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
                modifier: Modifier,
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
                            imageVector = Icons.Default.Folder,
                            contentScale = ContentScale.Fit,
                            contentDescription = null
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = folder.name,
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
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
                modifier: Modifier,
                playlist: State.Content.Loaded.Playlist,
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
                            id = null,
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
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
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
                modifier: Modifier,
                track: State.Content.Loaded.Track,
                onClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

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
                            id = track.image,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(track.artists) { artist ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    id = track.image
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
//                            Option(
//                                label = "Play next",
//                                icon = Icons.Default.QueuePlayNext,
//                                onClick = { TODO() },
//                            )
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
        }
    }
}
