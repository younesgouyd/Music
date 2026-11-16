package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
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
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.multiplatform.usecases.AddToPlaylistUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.ClearImportItemUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.DeleteUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.MoveItemsToFolderUseCase
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
    tagRepo: TagRepo,
    folderRepo: FolderRepo,
    playlistRepo: PlaylistRepo,
    trackRepo: TrackRepo,
    addToPlaylistUseCase: AddToPlaylistUseCase,
    deleteUseCase: DeleteUseCase,
    clearImportItemUseCase: ClearImportItemUseCase,
    moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
    mediaController: MediaController,
    showImportFlow: (FolderId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Library"
    private val path = MutableStateFlow(listOf(Ui.State.NodeState(null)))
    private val currentFolder: StateFlow<Folder?> = path.map { it.last().folder }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    private val selectedTags = MutableStateFlow(emptyList<TagId>())
    private val selectedItems = MutableStateFlow(emptySet<SelectedItem>())
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
            selectedItems = selectedItems.asStateFlow(),
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
                        if (enableFiltering) {
                            trackRepo.searchFolderWithTags(currentFolder?.id, searchQuery, selectedTags, includeUntagged)
                        } else {
                            trackRepo.searchFolder(currentFolder?.id, searchQuery)
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
                }.onEach {
                    selectedItems.value = emptySet()
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Content.Loading)
            },
            actions = Ui.State.Actions(
                onImport = { showImportFlow(path.value.last().folder!!.id) },
                onSearchQueryChange = { searchQuery.value = it },
                onNewFolder = {
                    coroutineScope.launch {
                        folderRepo.add(name = it, parentFolderId = currentFolder.value?.id)
                    }
                },

                onOpenFolder = { folder ->
                    path.update { list ->
                        if (folder == null) return@update list.take(1)
                        val i = list.indexOfFirst { it.folder?.id == folder.id }
                        if (i >= 0) list.take(i + 1) else list + Ui.State.NodeState(folder)
                    }
                },
                onPlaylistClick = showPlaylist,
                onTrackClick = showTrack,
                onArtistClick = showArtistDetails,

                onToggleSelection = { item ->
                    selectedItems.update { set ->
                        if (set.contains(item)) set - item else set + item
                    }
                },
                onClearSelection = {
                    selectedItems.value = emptySet()
                },

                onMoveSelectedToFolder = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        val (tracks, playlists, folders) = items.toTriple()
                        moveToFolder.update {
                            MoveToFolder(
                                startFolder = currentFolder.value?.id,
                                tracks = tracks.toSet(),
                                playlists = playlists.toSet(),
                                folders = folders.toSet(),
                                folderRepo = folderRepo,
                                moveItemsToFolderUseCase = moveItemsToFolderUseCase,
                                dismiss = ::dismissMoveToFolderDialog
                            )
                        }
                        selectedItems.value = emptySet()
                        moveToFolderDialogVisible.value = true
                    }
                },
                onMoveFolderToFolder = { id: FolderId ->
                    moveToFolder.update {
                        MoveToFolder(
                            startFolder = currentFolder.value?.id,
                            tracks = emptySet(),
                            playlists = emptySet(),
                            folders = setOf(id),
                            folderRepo = folderRepo,
                            moveItemsToFolderUseCase = moveItemsToFolderUseCase,
                            dismiss = ::dismissMoveToFolderDialog
                        )
                    }
                    selectedItems.value = emptySet()
                    moveToFolderDialogVisible.value = true
                },
                onMovePlaylistToFolder = { id: PlaylistId ->
                    moveToFolder.update {
                        MoveToFolder(
                            startFolder = currentFolder.value?.id,
                            tracks = emptySet(),
                            playlists = setOf(id),
                            folders = emptySet(),
                            folderRepo = folderRepo,
                            moveItemsToFolderUseCase = moveItemsToFolderUseCase,
                            dismiss = ::dismissMoveToFolderDialog
                        )
                    }
                    selectedItems.value = emptySet()
                    moveToFolderDialogVisible.value = true
                },
                onMoveTrackToFolder = { id: TrackId ->
                    moveToFolder.update {
                        MoveToFolder(
                            startFolder = currentFolder.value?.id,
                            tracks = setOf(id),
                            playlists = emptySet(),
                            folders = emptySet(),
                            folderRepo = folderRepo,
                            moveItemsToFolderUseCase = moveItemsToFolderUseCase,
                            dismiss = ::dismissMoveToFolderDialog
                        )
                    }
                    selectedItems.value = emptySet()
                    moveToFolderDialogVisible.value = true
                },

                onAddSelectedToPlaylist = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        val (tracks, playlists, folders) = items.toTriple()
                        addToPlaylist.update {
                            if (it != null) TODO()
                            AddToPlaylist(
                                tracks = tracks.toList(),
                                playlists = playlists.toList(),
                                folders = folders.toList(),
                                artists = emptyList(),
                                albums = emptyList(),
                                playlistRepo = playlistRepo,
                                addToPlaylistUseCase = addToPlaylistUseCase,
                                dismiss = ::dismissAddToPlaylistDialog
                            )
                        }
                        selectedItems.value = emptySet()
                        addToPlaylistDialogVisible.value = true
                    }
                },
                onAddFolderToPlaylist = { folderId: FolderId ->
                    addToPlaylist.update {
                        if (it != null) TODO()
                        AddToPlaylist(
                            tracks = emptyList(),
                            playlists = emptyList(),
                            folders = listOf(folderId),
                            artists = emptyList(),
                            albums = emptyList(),
                            playlistRepo = playlistRepo,
                            addToPlaylistUseCase = addToPlaylistUseCase,
                            dismiss = ::dismissAddToPlaylistDialog
                        )
                    }
                    addToPlaylistDialogVisible.value = true
                    selectedItems.value = emptySet()
                },
                onAddPlaylistToPlaylist = { playlistId: PlaylistId ->
                    addToPlaylist.update {
                        if (it != null) TODO()
                        AddToPlaylist(
                            tracks = emptyList(),
                            playlists = listOf(playlistId),
                            folders = emptyList(),
                            artists = emptyList(),
                            albums = emptyList(),
                            playlistRepo = playlistRepo,
                            addToPlaylistUseCase = addToPlaylistUseCase,
                            dismiss = ::dismissAddToPlaylistDialog
                        )
                    }
                    addToPlaylistDialogVisible.value = true
                    selectedItems.value = emptySet()
                },
                onAddTrackToPlaylist = { trackId: TrackId ->
                    addToPlaylist.update {
                        if (it != null) TODO()
                        AddToPlaylist(
                            tracks = listOf(trackId),
                            playlists = emptyList(),
                            folders = emptyList(),
                            artists = emptyList(),
                            albums = emptyList(),
                            playlistRepo = playlistRepo,
                            addToPlaylistUseCase = addToPlaylistUseCase,
                            dismiss = ::dismissAddToPlaylistDialog
                        )
                    }
                    selectedItems.value = emptySet()
                    addToPlaylistDialogVisible.value = true
                },

                onAddSelectedToQueue = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        mediaController.addToQueue(
                            selectedItems.value.map {
                                when (it) {
                                    is SelectedItem.Track -> QueueItemParameter.Track(it.value.id)
                                    is SelectedItem.Playlist -> QueueItemParameter.Playlist(it.value.id)
                                    is SelectedItem.Folder -> QueueItemParameter.Folder(it.value.id)
                                }
                            }
                        )
                    }
                    selectedItems.value = emptySet()
                },
                onAddFolderToQueue = { id: FolderId ->
                    mediaController.addToQueue(
                        listOf(QueueItemParameter.Folder(id))
                    )
                    selectedItems.value = emptySet()
                },
                onAddPlaylistToQueue = { id: PlaylistId ->
                    mediaController.addToQueue(
                        listOf(QueueItemParameter.Playlist(id))
                    )
                    selectedItems.value = emptySet()
                },
                onAddTrackToQueue = {
                    mediaController.addToQueue(
                        listOf(QueueItemParameter.Track(it))
                    )
                    selectedItems.value = emptySet()
                },

                onPlaySelected = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        mediaController.playQueue(
                            selectedItems.value.map {
                                when (it) {
                                    is SelectedItem.Track -> QueueItemParameter.Track(it.value.id)
                                    is SelectedItem.Playlist -> QueueItemParameter.Playlist(it.value.id)
                                    is SelectedItem.Folder -> QueueItemParameter.Folder(it.value.id)
                                }
                            }
                        )
                        selectedItems.value = emptySet()
                    }
                },
                onPlayFolder = { id: FolderId ->
                    mediaController.playQueue(
                        listOf(QueueItemParameter.Folder(id))
                    )
                    selectedItems.value = emptySet()
                },
                onPlayPlaylist = { id: PlaylistId ->
                    mediaController.playQueue(
                        listOf(QueueItemParameter.Playlist(id))
                    )
                    selectedItems.value = emptySet()
                },

                onRenameFolder = { id: FolderId, name: String ->
                    coroutineScope.launch {
                        folderRepo.updateName(
                            id = id,
                            name = name
                        )
                        selectedItems.value = emptySet()
                    }
                },
                onRenamePlaylist = { id: PlaylistId, name: String ->
                    coroutineScope.launch {
                        playlistRepo.updateName(
                            id = id,
                            name = name
                        )
                        selectedItems.value = emptySet()
                    }
                },

                onDeleteSelected = {
                    coroutineScope.launch {
                        val items = selectedItems.value
                        if (items.isNotEmpty()) {
                            val (tracks, playlists, folders) = items.toTriple()
                            deleteUseCase.execute(
                                tracks = tracks,
                                playlists = playlists,
                                folders = folders
                            )
                        }
                        selectedItems.value = emptySet()
                    }
                },
                onDeleteFolder = { folderId ->
                    coroutineScope.launch {
                        deletingFolder.value = true
                        deleteUseCase.execute(
                            tracks = emptySet(),
                            playlists = emptySet(),
                            folders = setOf(folderId)
                        )
                        selectedItems.value = emptySet()
                        deletingFolder.value = false
                    }
                },
                onDeletePlaylist = { playlistId ->
                    coroutineScope.launch {
                        deleteUseCase.execute(
                            tracks = emptySet(),
                            playlists = setOf(playlistId),
                            folders = emptySet()
                        )
                        selectedItems.value = emptySet()
                    }
                },
                onDeleteTrack = {
                    coroutineScope.launch {
                        clearImportItemUseCase.execute(it)
                        selectedItems.value = emptySet()
                    }
                },
            )
        )
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
                    uiState.actions.onOpenFolder(path[path.size - 2].folder)
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
    
    private fun Set<SelectedItem>.toTriple(): Triple<Set<TrackId>, Set<PlaylistId>, Set<FolderId>> {
        val tracks = mutableSetOf<TrackId>()
        val playlists = mutableSetOf<PlaylistId>()
        val folders = mutableSetOf<FolderId>()
        for (item in this) {
            when (item) {
                is SelectedItem.Track -> tracks.add(item.value.id)
                is SelectedItem.Playlist -> playlists.add(item.value.id)
                is SelectedItem.Folder -> folders.add(item.value.id)
            }
        }
        return Triple(tracks, playlists, folders)
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

    private sealed class SelectedItem {
        data class Track(val value: Ui.State.Content.Loaded.Track) : SelectedItem()
        data class Playlist(val value: Ui.State.Content.Loaded.Playlist) : SelectedItem()
        data class Folder(val value: dev.younesgouyd.apps.music.common.models.datamodels.Folder) : SelectedItem()
    }

    private object Ui {
        class State(
            val path: StateFlow<List<NodeState>>,
            val searchQuery: StateFlow<String>,
            val selectedItems: StateFlow<Set<SelectedItem>>,
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
                val onImport: () -> Unit,
                val onSearchQueryChange: (String) -> Unit,
                val onNewFolder: (name: String) -> Unit,

                val onOpenFolder: (Folder?) -> Unit,
                val onPlaylistClick: (PlaylistId) -> Unit,
                val onTrackClick: (TrackId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,

                val onToggleSelection: (SelectedItem) -> Unit,
                val onClearSelection: () -> Unit,

                val onMoveSelectedToFolder: () -> Unit,
                val onMoveFolderToFolder: (id: FolderId) -> Unit,
                val onMovePlaylistToFolder: (id: PlaylistId) -> Unit,
                val onMoveTrackToFolder: (id: TrackId) -> Unit,

                val onAddSelectedToPlaylist: () -> Unit,
                val onAddFolderToPlaylist: (FolderId) -> Unit,
                val onAddPlaylistToPlaylist: (PlaylistId) -> Unit,
                val onAddTrackToPlaylist: (TrackId) -> Unit,

                val onAddSelectedToQueue: () -> Unit,
                val onAddFolderToQueue: (FolderId) -> Unit,
                val onAddPlaylistToQueue: (PlaylistId) -> Unit,
                val onAddTrackToQueue: (TrackId) -> Unit,

                val onPlaySelected: () -> Unit,
                val onPlayFolder: (FolderId) -> Unit,
                val onPlayPlaylist: (PlaylistId) -> Unit,

                val onRenameFolder: (id: FolderId, name: String) -> Unit,
                val onRenamePlaylist: (id: PlaylistId, name: String) -> Unit,

                val onDeleteSelected: () -> Unit,
                val onDeleteFolder: (FolderId) -> Unit,
                val onDeletePlaylist: (PlaylistId) -> Unit,
                val onDeleteTrack: (ImportSessionItemId) -> Unit
            )
        }

        @Composable
        fun Content(
            modifier: Modifier,
            content: State.Content,
            gridState: LazyGridState,
            actions: State.Actions,
            selectedItems: Set<SelectedItem>,
            folderUi: @Composable (Modifier, Folder, isSelected: Boolean, State.Actions) -> Unit,
            playlistUi: @Composable (Modifier, State.Content.Loaded.Playlist, isSelected: Boolean, State.Actions) -> Unit,
            trackUi: @Composable (Modifier, State.Content.Loaded.Track, isSelected: Boolean, State.Actions) -> Unit
        ) {
            val minColumnWidth: Dp = when (getWindowSizeClass()) {
                WindowWidthSizeClass.Compact -> 100.dp
                else -> 200.dp
            }

            when (content) {
                is State.Content.Loading -> {
                    Box(modifier.padding(10.dp), Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is State.Content.Loaded -> {
                    LazyVerticalGrid(
                        modifier = modifier,
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        columns = GridCells.Adaptive(minColumnWidth)
                    ) {
                        val ratio = 0.65f
                        items(content.folders, { "folder#${it.id}" }) { folder ->
                            val isSelected = selectedItems.any {
                                it is SelectedItem.Folder && it.value.id == folder.id
                            }
                            folderUi(Modifier.aspectRatio(ratio), folder, isSelected, actions)
                        }
                        items(content.playlists, { "playlist#${it.id}" }) { playlist ->
                            val isSelected = selectedItems.any {
                                it is SelectedItem.Playlist && it.value.id == playlist.id
                            }
                            playlistUi(Modifier.aspectRatio(ratio), playlist, isSelected, actions)
                        }
                        items(content.tracks, { "track#${it.id}" }) { track ->
                            val isSelected = selectedItems.any {
                                it is SelectedItem.Track && it.value.id == track.id
                            }
                            trackUi(Modifier.aspectRatio(ratio), track, isSelected, actions)
                        }
                    }
                }
            }
        }

        @Composable
        private fun ContextMenu(
            modifier: Modifier,
            selectedItems: Set<SelectedItem>,
            onCancel: () -> Unit,
            onPlay: () -> Unit,
            onMove: () -> Unit,
            onRename: (SelectedItem, String) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onDelete: () -> Unit
        ) {
            var showMore by remember { mutableStateOf(false)}

            ElevatedCard(
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancel,
                            content = { Icon(Icons.Default.Close, "Cancel") }
                        )
                        Text(
                            text = "${selectedItems.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMove,
                            content = { Icon(Icons.AutoMirrored.Default.DriveFileMove, "Move") }
                        )
                        IconButton(
                            onClick = onAddToPlaylistClick,
                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, "Add to playlist") }
                        )
                        IconButton(
                            onClick = onPlay,
                            content = { Icon(Icons.Default.PlayCircle, "Play") }
                        )
                        IconButton(
                            onClick = { showMore = true },
                            content = { Icon(Icons.Default.MoreVert, "More") }
                        )
                    }
                }
            }

            if (showMore) {
                var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

                Dialog(onDismissRequest = { showMore = false }) {
                    Surface(
                        modifier = Modifier.width(400.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            if (selectedItems.size == 1 && selectedItems.first() !is SelectedItem.Track) {
                                var showEditFormDialog by remember { mutableStateOf(false) }
                                val item = selectedItems.first()
                                val (title, name) = remember {
                                    when (item) {
                                        is SelectedItem.Folder -> Pair("Rename folder: ${item.value.name}", item.value.name)
                                        is SelectedItem.Playlist -> Pair("Rename folder: ${item.value.name}", item.value.name)
                                        is SelectedItem.Track -> TODO()
                                    }
                                }
                                Option(
                                    label = "Rename",
                                    icon = Icons.Default.Edit,
                                    onClick = { showEditFormDialog = true },
                                )
                                if (showEditFormDialog) {
                                    RenameDialog(
                                        title = title,
                                        name = name,
                                        onDone = {
                                            onRename(item, it)
                                            showEditFormDialog = false
                                            showMore = false
                                        },
                                        onDismiss = { showEditFormDialog = false }
                                    )
                                }
                            }
                            Option(
                                label = "Add to queue",
                                icon = Icons.Default.AddToQueue,
                                onClick = onAddToQueueClick
                            )
                            Option(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = { showDeleteConfirmationDialog = true },
                            )
                        }
                    }
                }

                if (showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        message = "Delete ${selectedItems.size} items?",
                        onDismissRequest = { showDeleteConfirmationDialog = false },
                        onYesClick = {
                            showDeleteConfirmationDialog = false
                            onDelete()
                        }
                    )
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
                    selectedItems = state.selectedItems,
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
                selectedItems: StateFlow<Set<SelectedItem>>,
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
                val selectedItems by selectedItems.collectAsState()

                DisposableEffect(node) {
                    onDispose {
                        node.firstVisibleItemIndex = gridState.firstVisibleItemIndex
                        node.firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
                    }
                }

                Scaffold(
                    modifier = modifier,
                    content = { paddingValues ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ToolBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    path = path.map { it.folder },
                                    searchQuery = searchQuery,
                                    tagsFilterState = tagsFilterState,
                                    onFolderClick = actions.onOpenFolder,
                                    onImportClick = actions.onImport,
                                    onNewFolder = actions.onNewFolder,
                                    onSearchQueryChange = actions.onSearchQueryChange,
                                )
                                Content(
                                    modifier = Modifier.fillMaxSize(),
                                    content = content,
                                    gridState = gridState,
                                    actions = actions,
                                    selectedItems = selectedItems,
                                    folderUi = { modifier, folder, isSelected, actions ->
                                        FolderItem(
                                            modifier = modifier,
                                            folder = folder,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Folder(folder))
                                                } else {
                                                    actions.onOpenFolder(folder)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Folder(folder)) },
                                            onMoveToFolder = { actions.onMoveFolderToFolder(folder.id) },
                                            onAddToPlaylistClick = { actions.onAddFolderToPlaylist(folder.id) },
                                            onAddToQueueClick = { actions.onAddFolderToQueue(folder.id) },
                                            onPlayClick = { actions.onPlayFolder(folder.id) },
                                            onRenameClick = { actions.onRenameFolder(folder.id, it) },
                                            onDeleteClick = { actions.onDeleteFolder(folder.id) }
                                        )
                                    },
                                    playlistUi = { modifier, playlist, isSelected, actions ->
                                        PlaylistItem(
                                            modifier = modifier,
                                            playlist = playlist,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Playlist(playlist))
                                                } else {
                                                    actions.onPlaylistClick(playlist.id)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Playlist(playlist)) },
                                            onPlayClick = { actions.onPlayPlaylist(playlist.id) },
                                            onMoveToFolder = { actions.onMovePlaylistToFolder(playlist.id) },
                                            onAddToPlaylistClick = { actions.onAddPlaylistToPlaylist(playlist.id) },
                                            onAddToQueueClick = { actions.onAddPlaylistToQueue(playlist.id) },
                                            onRenameClick = { actions.onRenamePlaylist(playlist.id, it) },
                                            onDeleteClick = { actions.onDeletePlaylist(playlist.id) }
                                        )
                                    },
                                    trackUi = { modifier, track, isSelected, actions ->
                                        TrackItem(
                                            modifier = modifier,
                                            track = track,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Track(track))
                                                } else {
                                                    actions.onTrackClick(track.id)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Track(track)) },
                                            onMoveToFolder = { actions.onMoveTrackToFolder(track.id) },
                                            onAddToPlaylistClick = { actions.onAddTrackToPlaylist(track.id) },
                                            onArtistClick = actions.onArtistClick,
                                            onDeleteClick = { actions.onDeleteTrack(track.importSessionItemId) },
                                            onAddToQueueClick = { actions.onAddTrackToQueue(track.id) }
                                        )
                                    }
                                )
                            }
                            if (selectedItems.isNotEmpty()) {
                                ContextMenu(
                                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -50.dp),
                                    selectedItems = selectedItems,
                                    onCancel = actions.onClearSelection,
                                    onMove = actions.onMoveSelectedToFolder,
                                    onPlay = actions.onPlaySelected,
                                    onRename = { selected, name ->
                                        when (selected) {
                                            is SelectedItem.Track -> TODO()
                                            is SelectedItem.Playlist -> actions.onRenamePlaylist(selected.value.id, name)
                                            is SelectedItem.Folder -> actions.onRenameFolder(selected.value.id, name)
                                        }
                                    },
                                    onAddToPlaylistClick = actions.onAddSelectedToPlaylist,
                                    onAddToQueueClick = actions.onAddSelectedToQueue,
                                    onDelete = actions.onDeleteSelected
                                )
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(gridState) }
                )
            }

            @Composable
            private fun ToolBar(
                modifier: Modifier,
                path: List<Folder?>,
                searchQuery: StateFlow<String>,
                tagsFilterState: TagsFilterState,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit
            ) {
                val pathLazyListState = rememberLazyListState()
                var newFolderFormVisible by remember { mutableStateOf(false) }

                LaunchedEffect(path) {
                    if (path.isNotEmpty()) {
                        pathLazyListState.animateScrollToItem(path.size - 1)
                    }
                }

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
                        Path(
                            modifier = Modifier.weight(1f),
                            path = path,
                            searchQuery = searchQuery,
                            onFolderClick = onFolderClick,
                            onSearchQueryChange = onSearchQueryChange
                        )
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (path.size > 1) {
                                IconButton(
                                    onClick = onImportClick,
                                    content = { Icon(Icons.Default.ImportExport, null) }
                                )
                            }
                            IconButton(
                                onClick = { newFolderFormVisible = true },
                                content = { Icon(Icons.Default.CreateNewFolder, null) }
                            )
                            VerticalDivider()
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
            }

            @Composable
            private fun FolderItem(
                modifier: Modifier,
                folder: Folder,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onPlayClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }

                if (showContextMenu) {
                    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                    var showEditFormDialog by remember { mutableStateOf(false) }

                    ItemContextMenu(
                        item = Item(folder.name, null),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = onPlayClick,
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
                            icon = Icons.AutoMirrored.Default.DriveFileMove,
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
                            onClick = onAddToQueueClick
                        )
                    }

                    if (showEditFormDialog) {
                        RenameDialog(
                            title = "Rename folder: ${folder.name}",
                            name = folder.name,
                            onDone = {
                                onRenameClick(it)
                                showEditFormDialog = false
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
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            @Composable
            private fun PlaylistItem(
                modifier: Modifier,
                playlist: State.Content.Loaded.Playlist,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
                onPlayClick: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onAddToQueueClick: () -> Unit,
                onRenameClick: (name: String) -> Unit,
                onDeleteClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }

                if (showContextMenu) {
                    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                    var showEditFormDialog by remember { mutableStateOf(false) }

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
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier,
                track: State.Content.Loaded.Track,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
                onMoveToFolder: () -> Unit,
                onAddToPlaylistClick: () -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onDeleteClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }

                if (showContextMenu) {
                    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

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
                    selectedItems = state.selectedItems,
                    content = state.content,
                    actions = state.actions
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                path: StateFlow<List<State.NodeState>>,
                searchQuery: StateFlow<String>,
                selectedItems: StateFlow<Set<SelectedItem>>,
                content: StateFlow<State.Content>,
                actions: State.Actions
            ) {
                val path by path.collectAsState()
                val content by content.collectAsState()
                val node = path.last()
                val gridState = remember(node) {
                    LazyGridState(node.firstVisibleItemIndex, node.firstVisibleItemScrollOffset)
                }
                val selectedItems by selectedItems.collectAsState()

                DisposableEffect(node) {
                    onDispose {
                        node.firstVisibleItemIndex = gridState.firstVisibleItemIndex
                        node.firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
                    }
                }

                Scaffold(
                    modifier = modifier,
                    content = { paddingValues ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ToolBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    path = path.map { it.folder },
                                    searchQuery = searchQuery,
                                    onFolderClick = actions.onOpenFolder,
                                    onImportClick = actions.onImport,
                                    onNewFolder = actions.onNewFolder,
                                    onSearchQueryChange = actions.onSearchQueryChange
                                )
                                Content(
                                    modifier = Modifier.fillMaxSize(),
                                    content = content,
                                    gridState = gridState,
                                    actions = actions,
                                    selectedItems = selectedItems,
                                    folderUi = { modifier, folder, isSelected, actions ->
                                        FolderItem(
                                            modifier = modifier,
                                            folder = folder,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Folder(folder))
                                                } else {
                                                    actions.onOpenFolder(folder)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Folder(folder)) }
                                        )
                                    },
                                    playlistUi = { modifier, playlist, isSelected, actions ->
                                        PlaylistItem(
                                            modifier = modifier,
                                            playlist = playlist,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Playlist(playlist))
                                                } else {
                                                    actions.onPlaylistClick(playlist.id)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Playlist(playlist)) },
                                        )
                                    },
                                    trackUi = { modifier, track, isSelected, actions ->
                                        TrackItem(
                                            modifier = modifier,
                                            track = track,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedItems.isNotEmpty()) {
                                                    actions.onToggleSelection(SelectedItem.Track(track))
                                                } else {
                                                    actions.onTrackClick(track.id)
                                                }
                                            },
                                            onToggleSelection = { actions.onToggleSelection(SelectedItem.Track(track)) },
                                        )
                                    }
                                )
                            }
                            if (selectedItems.isNotEmpty()) {
                                ContextMenu(
                                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -50.dp),
                                    selectedItems = selectedItems,
                                    onCancel = actions.onClearSelection,
                                    onMove = actions.onMoveSelectedToFolder,
                                    onPlay = actions.onPlaySelected,
                                    onRename = { selected, name ->
                                        when (selected) {
                                            is SelectedItem.Track -> TODO()
                                            is SelectedItem.Playlist -> actions.onRenamePlaylist(selected.value.id, name)
                                            is SelectedItem.Folder -> actions.onRenameFolder(selected.value.id, name)
                                        }
                                    },
                                    onAddToPlaylistClick = actions.onAddSelectedToPlaylist,
                                    onAddToQueueClick = actions.onAddSelectedToQueue,
                                    onDelete = actions.onDeleteSelected
                                )
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(gridState) }
                )
            }

            @Composable
            private fun ToolBar(
                modifier: Modifier,
                path: List<Folder?>,
                searchQuery: StateFlow<String>,
                onFolderClick: (Folder?) -> Unit,
                onImportClick: () -> Unit,
                onNewFolder: (name: String) -> Unit,
                onSearchQueryChange: (String) -> Unit
            ) {
                val pathLazyListState = rememberLazyListState()
                var newFolderFormVisible by remember { mutableStateOf(false) }

                LaunchedEffect(path) {
                    if (path.isNotEmpty()) {
                        pathLazyListState.animateScrollToItem(path.size - 1)
                    }
                }

                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Path(
                        modifier = Modifier.fillMaxWidth(),
                        path = path,
                        searchQuery = searchQuery,
                        onFolderClick = onFolderClick,
                        onSearchQueryChange = onSearchQueryChange
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (path.size > 1) {
                            IconButton(
                                onClick = onImportClick,
                                content = { Icon(Icons.Default.ImportExport, null) }
                            )
                        }
                        IconButton(
                            onClick = { newFolderFormVisible = true },
                            content = { Icon(Icons.Default.CreateNewFolder, null) }
                        )
                        VerticalDivider()
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

                if (newFolderFormVisible) {
                    RenameDialog(
                        title = "New folder",
                        onDone = { onNewFolder(it); newFolderFormVisible = false },
                        onDismiss = { newFolderFormVisible = false }
                    )
                }
            }

            @Composable
            private fun FolderItem(
                modifier: Modifier,
                folder: Folder,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = onToggleSelection,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun PlaylistItem(
                modifier: Modifier,
                playlist: State.Content.Loaded.Playlist,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = onToggleSelection,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun TrackItem(
                modifier: Modifier,
                track: State.Content.Loaded.Track,
                isSelected: Boolean,
                onClick: () -> Unit,
                onToggleSelection: () -> Unit,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = onToggleSelection,
                    isSelected = isSelected,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                        if (isHovered || isSelected) {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                onClick = onToggleSelection,
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckBox, null)
                                } else {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
