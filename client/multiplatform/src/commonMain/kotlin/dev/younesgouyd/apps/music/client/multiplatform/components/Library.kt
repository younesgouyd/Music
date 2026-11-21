package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import dev.younesgouyd.apps.music.client.multiplatform.usecases.DeleteUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.MoveItemsToFolderUseCase
import dev.younesgouyd.apps.music.client.multiplatform.usecases.TagItemsUseCase
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.Folder
import dev.younesgouyd.apps.music.common.models.datamodels.Playlist
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class Library(
    private val tagRepo: TagRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val addToPlaylistUseCase: AddToPlaylistUseCase,
    private val deleteUseCase: DeleteUseCase,
    private val moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
    private val tagItemsUseCase: TagItemsUseCase,
    private val mediaController: MediaController,
    showImportFlow: (FolderId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showArtistDetails: (SpotifyArtistId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Library"
    private val currentFolder: MutableStateFlow<FolderId?> = MutableStateFlow(null)
    private val selectedTags = MutableStateFlow(emptyList<TagId>())
    private val selectedItems = MutableStateFlow(emptySet<SelectedItem>())
    private val loadingFolders = MutableStateFlow(true)
    private val loadingPlaylists = MutableStateFlow(true)
    private val loadingTracks = MutableStateFlow(true)
    private val searchQuery = MutableStateFlow("")
    private val enableFiltering = MutableStateFlow(false)
    private val includeUntagged = MutableStateFlow(false)
    private val dialogState: MutableStateFlow<Ui.State.DialogState?> = MutableStateFlow(null)
    private val notifications: Channel<String> = Channel(Channel.BUFFERED)
    private val state: Ui.State

    private data class TracksFilter(
        val currentFolder: FolderId?,
        val search: String,
        val tags: List<TagId>,
        val enableFiltering: Boolean,
        val includeUntagged: Boolean
    )

    private class ScrollState(
        var firstVisibleItemIndex: Int = 0,
        var firstVisibleItemScrollOffset: Int = 0
    )

    init {
        state = Ui.State(
            path = run {
                var scrollStates: Map<FolderId?, ScrollState> = mapOf()
                currentFolder.flatMapLatest {
                    if (it == null) flowOf(emptyList())
                    else folderRepo.getPath(it)
                }.map { path ->
                    val temp: MutableMap<FolderId?, ScrollState> = mutableMapOf()
                    val rootState = scrollStates[null] ?: ScrollState()
                    temp[null] = rootState
                    val new = path.map { folder ->
                        val id = folder.id
                        val scrollState = scrollStates[id] ?: ScrollState()
                        temp[id] = scrollState
                        Ui.State.NodeState(folder, scrollState)
                    }
                    scrollStates = temp
                    listOf(Ui.State.NodeState(null, rootState)) + new
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf(Ui.State.NodeState()))
            },
            searchQuery = this@Library.searchQuery.asStateFlow(),
            selectedItems = selectedItems.asStateFlow(),
            tagsFilterState = run {
                val searchQuery = MutableStateFlow("")
                TagsFilterState(
                    tags = combine(searchQuery.debounce(500.milliseconds), selectedTags, ::Pair)
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
                    searchQuery = searchQuery.asStateFlow(),
                    enableFiltering = enableFiltering.asStateFlow(),
                    includeUntagged = includeUntagged.asStateFlow(),
                    onSearchQueryChange = { searchQuery.value = it },
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
                )
            },
            content = run {
                val debouncedSearch = this.searchQuery.debounce(500.milliseconds)
                val folders = combine(currentFolder, debouncedSearch, ::Pair)
                    .onEach { loadingFolders.value = true }
                    .flatMapLatest { (currentFolder, search) -> folderRepo.searchFolder(currentFolder, search) }
                    .onEach { loadingFolders.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val playlists: StateFlow<List<Ui.State.Content.Loaded.Playlist>> = combine(currentFolder, debouncedSearch, ::Pair)
                    .onEach { loadingPlaylists.value = true }
                    .flatMapLatest { (currentFolder, search) -> playlistRepo.searchFolder(currentFolder, search) }
                    .map { dbPlaylists -> dbPlaylists.toPlaylistModels() }
                    .onEach { loadingPlaylists.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val tracks: StateFlow<List<Ui.State.Content.Loaded.Track>> = combine(
                    currentFolder, debouncedSearch,
                    selectedTags, enableFiltering,
                    includeUntagged, ::TracksFilter
                ).onEach { loadingTracks.value = true }
                    .flatMapLatest { (currentFolder, searchQuery, selectedTags, enableFiltering, includeUntagged) ->
                        if (enableFiltering) {
                            trackRepo.searchFolderWithTags(currentFolder, searchQuery, selectedTags, includeUntagged)
                        } else {
                            trackRepo.searchFolder(currentFolder, searchQuery)
                        }
                    }.mapLatest { dbTracks -> dbTracks.toTrackModels() }
                    .onEach { loadingTracks.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
                val loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks) { l1, l2, l3 -> l1 || l2 || l3 }
                combine(
                    loadingItems, currentFolder, folders, playlists, tracks
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
                }.stateIn(coroutineScope, SharingStarted.Eagerly, Ui.State.Content.Loading)
            },
            dialogState = dialogState.asStateFlow(),
            contextMenuState = selectedItems.map { selected ->
                if (selected.isEmpty()) null else Ui.State.ContextMenuState(
                    selectedItems = selected,
                    onCancel = { selectedItems.value = emptySet()},
                    onPlay = {
                        mediaController.playQueue(
                            selected.map { it.toQueueItemParameter() }
                        )
                        selectedItems.value = emptySet()
                    },
                    onRename = { showItemDialog(it, Ui.State.DialogType.Rename) },
                    onMove = { showDialog(Ui.State.DialogType.Move) },
                    onAddToPlaylistClick = { showDialog(Ui.State.DialogType.AddToPlaylist) },
                    onTag = {
                        if (selected.size == 1 && selected.first() is SelectedItem.Track) {
                            showItemDialog(selected.first(), Ui.State.DialogType.Tag)
                        } else {
                            showDialog(Ui.State.DialogType.Tag)
                        }
                    },
                    onAddToQueueClick = {
                        mediaController.addToQueue(
                            selected.map { it.toQueueItemParameter() }
                        )
                        selectedItems.value = emptySet()
                    },
                    onDelete = { showDialog(Ui.State.DialogType.DeleteConfirmation) }
                )
            }.stateIn(coroutineScope, SharingStarted.Eagerly, null),
            notifications = notifications.receiveAsFlow(),
            onImport = { showImportFlow(currentFolder.value!!) },
            onSearchQueryChange = { this@Library.searchQuery.value = it },
            onNewFolder = {
                coroutineScope.launch {
                    folderRepo.add(name = it, parentFolderId = currentFolder.value)
                }
            },
            onOpenFolder = { currentFolder.value = it },
            onPlaylistClick = showPlaylist,
            onTrackClick = showTrack,
            onArtistClick = showArtistDetails,
            onToggleSelection = { item ->
                selectedItems.update { set ->
                    if (set.contains(item)) set - item else set + item
                }
            },
            onShowItemDialog = ::showItemDialog,
            onPlay = { selected ->
                selectedItems.value = emptySet()
                dismissDialog()
                mediaController.playQueue(
                    listOf(selected.toQueueItemParameter())
                )
            },
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun dismissDialog() {
        dialogState.value = null
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
                name = dbPlaylist.name,
                image = null // TODO
            )
        }
    }

    private fun showDialog(type: Ui.State.DialogType) {
        dialogState.value = when (type) {
            Ui.State.DialogType.ContextMenu -> Ui.State.DialogState.ContextMenu(
                onDismiss = ::dismissDialog,
                onPlay = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        selectedItems.value = emptySet()
                        dismissDialog()
                        mediaController.playQueue(
                            items.map { it.toQueueItemParameter() }
                        )
                    }
                },
                onAddToQueue = {
                    val items = selectedItems.value
                    if (items.isNotEmpty()) {
                        selectedItems.value = emptySet()
                        dismissDialog()
                        mediaController.addToQueue(
                            items.map { it.toQueueItemParameter() }
                        )
                    }
                },
                onMove = { showDialog(Ui.State.DialogType.Move) },
                onAddToPlaylist = { showDialog(Ui.State.DialogType.AddToPlaylist) },
                onTag = { showDialog(Ui.State.DialogType.Tag) },
                onDelete = {
                    coroutineScope.launch {
                        val items = selectedItems.value
                        if (items.isNotEmpty()) {
                            val (tracks, playlists, folders) = items.toTriple()
                            selectedItems.value = emptySet()
                            dismissDialog()
                            deleteUseCase.execute(
                                tracks = tracks,
                                playlists = playlists,
                                folders = folders
                            )
                        }
                    }
                }
            )
            Ui.State.DialogType.Rename -> TODO()
            Ui.State.DialogType.Move -> {
                val items = selectedItems.value
                if (items.isNotEmpty()) {
                    val (tracks, playlists, folders) = items.toTriple()
                    createMoveDialogState(tracks, playlists, folders, true)
                } else null
            }
            Ui.State.DialogType.AddToPlaylist -> {
                val items = selectedItems.value
                if (items.isNotEmpty()) {
                    val (tracks, playlists, folders) = items.toTriple()
                    val atp = AddToPlaylist(
                        tracks = tracks,
                        playlists = playlists,
                        folders = folders,
                        playlistRepo = playlistRepo,
                        addToPlaylistUseCase = addToPlaylistUseCase,
                        onDone = {
                            selectedItems.value = emptySet()
                            dismissDialog()
                            clear()
                        }
                    )
                    Ui.State.DialogState.AddToPlaylist(
                        ui = atp::show,
                        onDismiss = {
                            if (atp.adding.value) {
                                return@AddToPlaylist
                            }
                            selectedItems.value = emptySet()
                            dismissDialog()
                            atp.clear()
                        }
                    )
                } else null
            }
            Ui.State.DialogType.Tag -> {
                val searchQuery = MutableStateFlow("")
                val selectedIds = MutableStateFlow(emptySet<TagId>())
                Ui.State.DialogState.TagBatch(
                    searchQuery = searchQuery.asStateFlow(),
                    tags = searchQuery.debounce(500.milliseconds)
                        .flatMapLatest { tagRepo.search(it) }
                        .combine(selectedIds) { dbTags, selectedIds ->
                            dbTags.map {
                                Ui.State.DialogState.Tag(
                                    id = it.id,
                                    name = it.name,
                                    isApplied = it.id in selectedIds
                                )
                            }
                        }
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                    onSearchQueryChange = { searchQuery.value = it },
                    onAdd = { coroutineScope.launch { tagRepo.add(searchQuery.value) } },
                    onTagClick = { clickedTagId ->
                        selectedIds.update {
                            if (clickedTagId in it) it - clickedTagId else it + clickedTagId
                        }
                    },
                    onDone = { unset ->
                        coroutineScope.launch {
                            val items = selectedItems.value
                            if (items.isNotEmpty()) {
                                val (tracks, playlists, folders) = items.toTriple()
                                selectedItems.value = emptySet()
                                dismissDialog()
                                tagItemsUseCase.execute(
                                    tracks = tracks,
                                    playlists = playlists,
                                    folders = folders,
                                    tags = selectedIds.value,
                                    unset = unset
                                )
                            }
                        }
                    },
                    onDismiss = ::dismissDialog
                )
            }
            Ui.State.DialogType.DeleteConfirmation -> Ui.State.DialogState.BatchDeleteConfirmation(
                onDelete = {
                    coroutineScope.launch {
                        val items = selectedItems.value
                        if (items.isNotEmpty()) {
                            val (tracks, playlists, folders) = items.toTriple()
                            selectedItems.value = emptySet()
                            dismissDialog()
                            deleteUseCase.execute(
                                tracks = tracks,
                                playlists = playlists,
                                folders = folders
                            )
                        }
                    }
                },
                onDismiss = ::dismissDialog
            )
        }
    }

    private fun showItemDialog(item: SelectedItem, type: Ui.State.DialogType) {
        dialogState.value = when (type) {
            Ui.State.DialogType.ContextMenu -> Ui.State.DialogState.ItemContextMenu(
                item = item,
                onPlay = {
                    dismissDialog()
                    mediaController.playQueue(
                        listOf(item.toQueueItemParameter())
                    )
                },
                onAddToQueue = {
                    dismissDialog()
                    mediaController.addToQueue(
                        listOf(item.toQueueItemParameter())
                    )
                },
                onRename = { showItemDialog(item, Ui.State.DialogType.Rename) },
                onMove = { showItemDialog(item, Ui.State.DialogType.Move) },
                onAddToPlaylist = { showItemDialog(item, Ui.State.DialogType.AddToPlaylist) },
                onTag = { showItemDialog(item, Ui.State.DialogType.Tag) },
                onDelete = {
                    coroutineScope.launch {
                        val (tracks, playlists, folders) = item.toTriple()
                        dismissDialog()
                        deleteUseCase.execute(
                            tracks = tracks,
                            playlists = playlists,
                            folders = folders
                        )
                    }
                },
                onDismiss = ::dismissDialog
            )
            Ui.State.DialogType.Rename -> Ui.State.DialogState.Rename(
                item = item,
                onRename = { newName ->
                    coroutineScope.launch {
                        dismissDialog()
                        when (item) {
                            is SelectedItem.Track -> TODO()
                            is SelectedItem.Playlist -> playlistRepo.updateName(item.value.id, newName)
                            is SelectedItem.Folder -> folderRepo.updateName(item.value.id, newName)
                        }
                    }
                },
                onDismiss = ::dismissDialog
            )
            Ui.State.DialogType.Move -> {
                val (tracks, playlists, folders) = item.toTriple()
                createMoveDialogState(tracks, playlists, folders, false)
            }
            Ui.State.DialogType.AddToPlaylist -> {
                val (tracks, playlists, folders) = item.toTriple()
                val atp = AddToPlaylist(
                    tracks = tracks,
                    playlists = playlists,
                    folders = folders,
                    artists = emptySet(),
                    albums = emptySet(),
                    playlistRepo = playlistRepo,
                    addToPlaylistUseCase = addToPlaylistUseCase,
                    onDone = {
                        dismissDialog()
                        clear()
                    }
                )
                Ui.State.DialogState.AddToPlaylist(
                    ui = atp::show,
                    onDismiss = {
                        if (atp.adding.value) {
                            return@AddToPlaylist
                        }
                        dismissDialog()
                        atp.clear()
                    }
                )
            }
            Ui.State.DialogType.Tag -> {
                when (item) {
                    is SelectedItem.Playlist, is SelectedItem.Folder -> {
                        val searchQuery = MutableStateFlow("")
                        val selectedIds = MutableStateFlow(emptySet<TagId>())
                        Ui.State.DialogState.TagBatch(
                            searchQuery = searchQuery.asStateFlow(),
                            tags = searchQuery.debounce(500.milliseconds)
                                .flatMapLatest { tagRepo.search(it) }
                                .combine(selectedIds) { dbTags, selectedIds ->
                                    dbTags.map {
                                        Ui.State.DialogState.Tag(
                                            id = it.id,
                                            name = it.name,
                                            isApplied = it.id in selectedIds
                                        )
                                    }
                                }
                                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                            onSearchQueryChange = { searchQuery.value = it },
                            onAdd = { coroutineScope.launch { tagRepo.add(searchQuery.value) } },
                            onTagClick = { clickedTagId ->
                                selectedIds.update {
                                    if (clickedTagId in it) it - clickedTagId else it + clickedTagId
                                }
                            },
                            onDone = { unset ->
                                coroutineScope.launch {
                                    val (tracks, playlists, folders) = item.toTriple()
                                    dismissDialog()
                                    tagItemsUseCase.execute(
                                        tracks = tracks,
                                        playlists = playlists,
                                        folders = folders,
                                        tags = selectedIds.value,
                                        unset = unset
                                    )
                                }
                            },
                            onDismiss = ::dismissDialog
                        )
                    }
                    is SelectedItem.Track -> {
                        val trackId = item.value.id
                        val searchQuery = MutableStateFlow("")
                        Ui.State.DialogState.TagTrack(
                            onDismiss = ::dismissDialog,
                            searchQuery = searchQuery.asStateFlow(),
                            item = item.value,
                            tags = searchQuery.debounce(500.milliseconds)
                                .flatMapLatest { search ->
                                    // TODO: implement this in server side repo
                                    combine(
                                        tagRepo.searchTrackTags(trackId, search),
                                        tagRepo.searchTrackUnsetTags(trackId, search),
                                        ::Pair
                                    ).map { (applied, nonapplied) ->
                                        val mapped1 = applied.map { Ui.State.DialogState.Tag(it.id, it.name, true) }
                                        val mapped2 = nonapplied.map { Ui.State.DialogState.Tag(it.id, it.name, false) }
                                        mapped1 + mapped2
                                    }
                                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                            onSearchQueryChange = { searchQuery.value = it },
                            onAdd = { coroutineScope.launch { tagRepo.add(searchQuery.value) } },
                            onTagClick = { tag ->
                                coroutineScope.launch {
                                    val (tracks, playlists, folders) = item.toTriple()
                                    tagItemsUseCase.execute(
                                        tracks = tracks,
                                        playlists = playlists,
                                        folders = folders,
                                        tags = setOf(tag.id),
                                        unset = tag.isApplied
                                    )
                                }
                            }
                        )
                    }
                }
            }
            Ui.State.DialogType.DeleteConfirmation -> Ui.State.DialogState.DeleteConfirmation(
                item = item,
                onDelete = {
                    coroutineScope.launch {
                        val (tracks, playlists, folders) = item.toTriple()
                        dismissDialog()
                        deleteUseCase.execute(
                            tracks = tracks,
                            playlists = playlists,
                            folders = folders
                        )
                    }
                },
                onDismiss = ::dismissDialog
            )
        }
    }

    private fun createMoveDialogState(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        isForMulti: Boolean
    ): Ui.State.DialogState.Move {
        val moving: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val searchQuery = MutableStateFlow("")
        val currentFolder: MutableStateFlow<FolderId?> = MutableStateFlow(currentFolder.value)
        val loadingFolders = MutableStateFlow(true)
        return Ui.State.DialogState.Move(
            moving = moving.asStateFlow(),
            path = currentFolder.flatMapLatest {
                if (it == null) flowOf(emptyList())
                else folderRepo.getPath(it)
            }.map { listOf(null) + it }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf(null)),
            searchQuery = searchQuery.asStateFlow(),
            folders = combine(currentFolder, searchQuery.debounce(500.milliseconds), ::Pair)
                .onEach { loadingFolders.value = true }
                .flatMapLatest { (folder, search) -> folderRepo.searchFolder(folder, search) }
                .onEach { loadingFolders.value = false }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            loadingFolders = loadingFolders.asStateFlow(),
            onSearchQueryChange = { searchQuery.value = it },
            onNewFolder = {
                coroutineScope.launch {
                    val id = folderRepo.add(name = it, parentFolderId = currentFolder.value)
                    currentFolder.value = id
                }
            },
            onFolderClick = { currentFolder.value = it },
            onMove = {
                coroutineScope.launch {
                    moving.update { true }
                    val result = moveItemsToFolderUseCase.execute(tracks, playlists, folders, currentFolder.value)
                    moving.update { false }
                    if (isForMulti) {
                        selectedItems.value = emptySet()
                    }
                    dismissDialog()
                    if (!result) {
                        notifications.trySend("Cannot move a folder into itself")
                    }
                }
            },
            onDismiss = {
                if (!moving.value) {
                    if (isForMulti) {
                        selectedItems.value = emptySet()
                    }
                    dismissDialog()
                }
            }
        )
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

    private fun SelectedItem.toTriple(): Triple<Set<TrackId>, Set<PlaylistId>, Set<FolderId>> {
        val tracks = mutableSetOf<TrackId>()
        val playlists = mutableSetOf<PlaylistId>()
        val folders = mutableSetOf<FolderId>()
        when (this) {
            is SelectedItem.Track -> tracks.add(this.value.id)
            is SelectedItem.Playlist -> playlists.add(this.value.id)
            is SelectedItem.Folder -> folders.add(this.value.id)
        }
        return Triple(tracks, playlists, folders)

    }

    private fun SelectedItem.toQueueItemParameter(): QueueItemParameter {
        return when (this) {
            is SelectedItem.Track -> QueueItemParameter.Track(this.value.id)
            is SelectedItem.Playlist -> QueueItemParameter.Playlist(this.value.id)
            is SelectedItem.Folder -> QueueItemParameter.Folder(this.value.id)
        }
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
            val tagsFilterState: TagsFilterState,
            val content: StateFlow<Content>,
            val selectedItems: StateFlow<Set<SelectedItem>>,
            val dialogState: StateFlow<DialogState?>,
            val contextMenuState: StateFlow<ContextMenuState?>,
            val notifications: Flow<String>,
            val onImport: () -> Unit,
            val onSearchQueryChange: (String) -> Unit,
            val onNewFolder: (name: String) -> Unit,
            val onOpenFolder: (FolderId?) -> Unit,
            val onPlaylistClick: (PlaylistId) -> Unit,
            val onTrackClick: (TrackId) -> Unit,
            val onArtistClick: (SpotifyArtistId) -> Unit,
            val onToggleSelection: (SelectedItem) -> Unit,
            val onShowItemDialog: (SelectedItem, DialogType) -> Unit,
            val onPlay: (SelectedItem) -> Unit
        ) {
            class NodeState(
                val folder: Folder? = null,
                val scrollState: ScrollState = ScrollState()
            )

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
                        val name: String,
                        val image: MediaFileId?
                    )
                }
            }

            sealed class DialogState {
                class ContextMenu(
                    val onPlay: () -> Unit,
                    val onAddToQueue: () -> Unit,
                    val onMove: () -> Unit,
                    val onAddToPlaylist: () -> Unit,
                    val onTag: () -> Unit,
                    val onDelete: () -> Unit,
                    val onDismiss: () -> Unit,
                ) : DialogState()

                class ItemContextMenu(
                    val item: SelectedItem,
                    val onPlay: () -> Unit,
                    val onAddToQueue: () -> Unit,
                    val onRename: () -> Unit,
                    val onMove: () -> Unit,
                    val onAddToPlaylist: () -> Unit,
                    val onTag: () -> Unit,
                    val onDelete: () -> Unit,
                    val onDismiss: () -> Unit,
                ) : DialogState()

                class Rename(
                    val item: SelectedItem,
                    val onRename: (String) -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class Move(
                    val moving: StateFlow<Boolean>,
                    val path: StateFlow<List<Folder?>>,
                    val searchQuery: StateFlow<String>,
                    val folders: StateFlow<List<Folder>>,
                    val loadingFolders: StateFlow<Boolean>,
                    val onSearchQueryChange: (String) -> Unit,
                    val onNewFolder: (name: String) -> Unit,
                    val onFolderClick: (FolderId?) -> Unit,
                    val onMove: () -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class AddToPlaylist(
                    val ui: @Composable () -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class TagBatch(
                    val searchQuery: StateFlow<String>,
                    val tags: StateFlow<List<Tag>>,
                    val onSearchQueryChange: (String) -> Unit,
                    val onAdd: () -> Unit,
                    val onTagClick: (TagId) -> Unit,
                    val onDone: (unset: Boolean) -> Unit,
                    val onDismiss: () -> Unit
                ): DialogState()

                class TagTrack(
                    val searchQuery: StateFlow<String>,
                    val item: Content.Loaded.Track,
                    val tags: StateFlow<List<Tag>>,
                    val onSearchQueryChange: (String) -> Unit,
                    val onAdd: () -> Unit,
                    val onTagClick: (State.DialogState.Tag) -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class BatchDeleteConfirmation(
                    val onDelete: () -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class DeleteConfirmation(
                    val item: SelectedItem,
                    val onDelete: () -> Unit,
                    val onDismiss: () -> Unit
                ) : DialogState()

                class Tag(
                    val id: TagId,
                    val name: String,
                    val isApplied: Boolean
                )
            }

            enum class DialogType {
                ContextMenu, Rename, Move, AddToPlaylist, Tag, DeleteConfirmation
            }

            data class ContextMenuState(
                val selectedItems: Set<SelectedItem>,
                val onCancel: () -> Unit,
                val onPlay: () -> Unit,
                val onRename: (SelectedItem) -> Unit,
                val onMove: () -> Unit,
                val onAddToPlaylistClick: () -> Unit,
                val onTag: () -> Unit,
                val onAddToQueueClick: () -> Unit,
                val onDelete: () -> Unit
            )
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            val dialogState by state.dialogState.collectAsState()

            DisposableEffect(Unit) {
                Application.registerBackHandler {
                    val path = state.path.value
                    if (path.size > 1) {
                        state.onOpenFolder(path[path.size - 2].folder?.id)
                    }
                }
                onDispose {
                    Application.unregisterLastBackHandler()
                }
            }

            Main(
                modifier = modifier,
                path = state.path,
                searchQuery = state.searchQuery,
                tagsFilterState = state.tagsFilterState,
                content = state.content,
                selectedItems = state.selectedItems,
                contextMenuState = state.contextMenuState,
                notifications = state.notifications,
                onImport = state.onImport,
                onSearchQueryChange = state.onSearchQueryChange,
                onNewFolder = state.onNewFolder,
                onOpenFolder = state.onOpenFolder,
                onPlaylistClick = state.onPlaylistClick,
                onTrackClick = state.onTrackClick,
                onArtistClick = state.onArtistClick,
                onToggleSelection = state.onToggleSelection,
                onShowItemDialog = state.onShowItemDialog,
                onPlay = state.onPlay
            )

            dialogState?.let { LibraryDialog(it) }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            path: StateFlow<List<State.NodeState>>,
            searchQuery: StateFlow<String>,
            tagsFilterState: TagsFilterState,
            content: StateFlow<State.Content>,
            selectedItems: StateFlow<Set<SelectedItem>>,
            contextMenuState: StateFlow<State.ContextMenuState?>,
            notifications: Flow<String>,
            onImport: () -> Unit,
            onSearchQueryChange: (String) -> Unit,
            onNewFolder: (name: String) -> Unit,
            onOpenFolder: (FolderId?) -> Unit,
            onPlaylistClick: (PlaylistId) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onToggleSelection: (SelectedItem) -> Unit,
            onShowItemDialog: (SelectedItem, State.DialogType) -> Unit,
            onPlay: (SelectedItem) -> Unit
        ) {
            val _path by path.collectAsState()
            val path = remember(_path) { _path.map { it.folder } }
            val scrollState = _path.last().scrollState
            val content by content.collectAsState()
            val selectedItems by selectedItems.collectAsState()
            val (selectedFolderIds, selectedPlaylistIds, selectedTrackIds) = remember(selectedItems) {
                val folders = HashSet<FolderId>()
                val playlists = HashSet<PlaylistId>()
                val tracks = HashSet<TrackId>()
                for (item in selectedItems) {
                    when (item) {
                        is SelectedItem.Folder -> folders.add(item.value.id)
                        is SelectedItem.Playlist -> playlists.add(item.value.id)
                        is SelectedItem.Track -> tracks.add(item.value.id)
                    }
                }
                Triple(folders, playlists, tracks)
            }
            val contextMenuState by contextMenuState.collectAsState()
            val gridState = remember(scrollState) {
                LazyGridState(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset)
            }
            val snackbarHostState = remember { SnackbarHostState() }
            val minColumnWidth: Dp = when (getWindowSizeClass()) {
                WindowWidthSizeClass.Compact -> 100.dp
                else -> 200.dp
            }

            DisposableEffect(scrollState) {
                onDispose {
                    scrollState.firstVisibleItemIndex = gridState.firstVisibleItemIndex
                    scrollState.firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
                }
            }

            LaunchedEffect(Unit) {
                notifications.collect {
                    snackbarHostState.showSnackbar(it)
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
                                path = path,
                                searchQuery = searchQuery,
                                tagsFilterState = tagsFilterState,
                                onFolderClick = onOpenFolder,
                                onImportClick = onImport,
                                onNewFolder = onNewFolder,
                                onSearchQueryChange = onSearchQueryChange,
                            )
                            when (val content = content) {
                                is State.Content.Loading -> {
                                    Box(Modifier.fillMaxSize().padding(10.dp), Alignment.Center) {
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
                                        items(content.folders, { "folder#${it.id}" }) { folder ->
                                            FolderItem(
                                                modifier = Modifier.aspectRatio(ratio),
                                                folder = folder,
                                                isSelected = folder.id in selectedFolderIds,
                                                onClick = {
                                                    if (selectedItems.isNotEmpty()) {
                                                        onToggleSelection(SelectedItem.Folder(folder))
                                                    } else {
                                                        onOpenFolder(folder.id)
                                                    }
                                                },
                                                onToggleSelection = { onToggleSelection(SelectedItem.Folder(folder)) },
                                                onPlayClick = { onPlay(SelectedItem.Folder(folder)) },
                                                onOptionsClick = { onShowItemDialog(SelectedItem.Folder(folder), State.DialogType.ContextMenu) }
                                            )
                                        }
                                        items(content.playlists, { "playlist#${it.id}" }) { playlist ->
                                            PlaylistItem(
                                                modifier = Modifier.aspectRatio(ratio),
                                                playlist = playlist,
                                                isSelected = playlist.id in selectedPlaylistIds,
                                                onClick = {
                                                    if (selectedItems.isNotEmpty()) {
                                                        onToggleSelection(SelectedItem.Playlist(playlist))
                                                    } else {
                                                        onPlaylistClick(playlist.id)
                                                    }
                                                },
                                                onToggleSelection = { onToggleSelection(SelectedItem.Playlist(playlist)) },
                                                onPlayClick = { onPlay(SelectedItem.Playlist(playlist)) },
                                                onOptionsClick = { onShowItemDialog(SelectedItem.Playlist(playlist), State.DialogType.ContextMenu) }
                                            )
                                        }
                                        items(content.tracks, { "track#${it.id}" }) { track ->
                                            TrackItem(
                                                modifier = Modifier.aspectRatio(ratio),
                                                track = track,
                                                isSelected = track.id in selectedTrackIds,
                                                onClick = {
                                                    if (selectedItems.isNotEmpty()) {
                                                        onToggleSelection(SelectedItem.Track(track))
                                                    } else {
                                                        onTrackClick(track.id)
                                                    }
                                                },
                                                onToggleSelection = { onToggleSelection(SelectedItem.Track(track)) },
                                                onArtistClick = onArtistClick,
                                                onOptionsClick = { onShowItemDialog(SelectedItem.Track(track), State.DialogType.ContextMenu) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        contextMenuState?.let { contextMenuState ->
                            ContextMenu(
                                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-50).dp),
                                selectedItems = contextMenuState.selectedItems,
                                onCancel = contextMenuState.onCancel,
                                onPlay = contextMenuState.onPlay,
                                onRename = contextMenuState.onRename,
                                onMove = contextMenuState.onMove,
                                onAddToPlaylistClick = contextMenuState.onAddToPlaylistClick,
                                onTag = contextMenuState.onTag,
                                onAddToQueueClick = contextMenuState.onAddToQueueClick,
                                onDelete = contextMenuState.onDelete
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = { ScrollToTopFloatingActionButton(gridState) }
            )
        }

        @Composable
        private fun ToolBar(
            modifier: Modifier,
            path: List<Folder?>,
            searchQuery: StateFlow<String>,
            tagsFilterState: TagsFilterState,
            onFolderClick: (FolderId?) -> Unit,
            onImportClick: () -> Unit,
            onNewFolder: (name: String) -> Unit,
            onSearchQueryChange: (String) -> Unit
        ) {
            var newFolderFormVisible by remember { mutableStateOf(false) }

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
        private fun ContextMenu(
            modifier: Modifier,
            selectedItems: Set<SelectedItem>,
            onCancel: () -> Unit,
            onPlay: () -> Unit,
            onRename: (SelectedItem) -> Unit,
            onMove: () -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onTag: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onDelete: () -> Unit
        ) {
            val scrollState = rememberScrollState()

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
                        modifier = Modifier.horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPlay,
                            content = { Icon(Icons.Default.PlayCircle, "Play") }
                        )
                        if (selectedItems.size == 1) {
                            val item = selectedItems.first()
                            if (item is SelectedItem.Playlist || item is SelectedItem.Folder) {
                                IconButton(
                                    onClick = { onRename(item) },
                                    content = { Icon(Icons.Default.Edit, "Rename") }
                                )
                            }
                        }
                        IconButton(
                            onClick = onMove,
                            content = { Icon(Icons.AutoMirrored.Default.DriveFileMove, "Move") }
                        )
                        IconButton(
                            onClick = onAddToPlaylistClick,
                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, "Add to playlist") }
                        )
                        IconButton(
                            onClick = onTag,
                            content = { Icon(Icons.Default.Tag, "Tag") }
                        )
                        IconButton(
                            onClick = onAddToQueueClick,
                            content = { Icon(Icons.Default.AddToQueue, "Add to queue") }
                        )
                        IconButton(
                            onClick = onDelete,
                            content = { Icon(Icons.Default.Delete, "Delete") }
                        )
                    }
                }
            }
        }

        @Composable
        private fun LibraryDialog(state: State.DialogState) {
            when (state) {
                is State.DialogState.ContextMenu -> ContextMenuDialog(
                    onPlay = state.onPlay,
                    onAddToQueue = state.onAddToQueue,
                    onMove = state.onMove,
                    onAddToPlaylist = state.onAddToPlaylist,
                    onTag = state.onTag,
                    onDelete = state.onDelete,
                    onDismiss = state.onDismiss
                )
                is State.DialogState.ItemContextMenu -> ItemContextMenuDialog(
                    item = state.item,
                    onPlay = state.onPlay,
                    onAddToQueue = state.onAddToQueue,
                    onRename = state.onRename,
                    onMove = state.onMove,
                    onAddToPlaylist = state.onAddToPlaylist,
                    onTag = state.onTag,
                    onDelete = state.onDelete,
                    onDismiss = state.onDismiss,
                )
                is State.DialogState.Rename -> {
                    val item = state.item
                    val (itemType, name) = when (item) {
                        is SelectedItem.Track -> "Track" to item.value.name
                        is SelectedItem.Playlist -> "Playlist" to item.value.name
                        is SelectedItem.Folder -> "Folder" to item.value.name
                    }
                    RenameDialog(
                        title = "Rename $itemType: $name",
                        name = name,
                        onDone = state.onRename,
                        onDismiss = state.onDismiss
                    )
                }
                is State.DialogState.Move -> MoveDialog(
                    moving = state.moving,
                    path = state.path,
                    searchQuery = state.searchQuery,
                    folders = state.folders,
                    loadingFolders = state.loadingFolders,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onNewFolder = state.onNewFolder,
                    onFolderClick = state.onFolderClick,
                    onMove = state.onMove,
                    onDismiss = state.onDismiss
                )
                is State.DialogState.AddToPlaylist -> Dialog(onDismissRequest = state.onDismiss, content = state.ui)
                is State.DialogState.TagBatch -> TagBatchDialog(
                    searchQuery = state.searchQuery,
                    tags = state.tags,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onAdd = state.onAdd,
                    onTagClick = state.onTagClick,
                    onDone = state.onDone,
                    onDismiss = state.onDismiss
                )
                is State.DialogState.TagTrack -> TagTrackDialog(
                    searchQuery = state.searchQuery,
                    item = state.item,
                    tags = state.tags,
                    onSearchQueryChange = state.onSearchQueryChange,
                    onAdd = state.onAdd,
                    onTagClick = state.onTagClick,
                    onDismiss = state.onDismiss
                )
                is State.DialogState.BatchDeleteConfirmation -> DeleteConfirmationDialog(
                    message = "Delete?", // TODO
                    onDismissRequest = state.onDismiss,
                    onYesClick = state.onDelete
                )
                is State.DialogState.DeleteConfirmation -> DeleteConfirmationDialog(
                    message = when (state.item) {
                        is SelectedItem.Track -> "Delete Track: ${state.item.value.name}?"
                        is SelectedItem.Playlist -> "Delete Playlist: ${state.item.value.name}?"
                        is SelectedItem.Folder -> "Delete Folder: ${state.item.value.name} including its content?"
                    },
                    onDismissRequest = state.onDismiss,
                    onYesClick = state.onDelete
                )
            }
        }

        @Composable
        private fun ContextMenuDialog(
            onPlay: () -> Unit,
            onAddToQueue: () -> Unit,
            onMove: () -> Unit,
            onAddToPlaylist: () -> Unit,
            onTag: () -> Unit,
            onDelete: () -> Unit,
            onDismiss: () -> Unit,
        ) {
            Dialog(onDismissRequest = onDismiss) {
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
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = onPlay
                        )
                        Option(
                            label = "Move",
                            icon = Icons.AutoMirrored.Default.DriveFileMove,
                            onClick = onMove
                        )
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylist
                        )
                        Option(
                            label = "Tag",
                            icon = Icons.Default.Tag,
                            onClick = onTag
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueue
                        )
                        Option(
                            label = "Delete",
                            icon = Icons.Default.Delete,
                            onClick = onDelete
                        )
                        Box(Modifier)
                    }
                }
            }
        }

        @Composable
        private fun ItemContextMenuDialog(
            item: SelectedItem,
            onPlay: () -> Unit,
            onAddToQueue: () -> Unit,
            onRename: () -> Unit,
            onMove: () -> Unit,
            onAddToPlaylist: () -> Unit,
            onTag: () -> Unit,
            onDelete: () -> Unit,
            onDismiss: () -> Unit,
        ) {
            Dialog(onDismissRequest = onDismiss) {
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
                        val (name, image) = when (item) {
                            is SelectedItem.Track -> item.value.name to item.value.image
                            is SelectedItem.Playlist -> item.value.name to item.value.image
                            is SelectedItem.Folder -> item.value.name to null
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(64.dp),
                                id = image
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = onPlay
                        )
                        if (item is SelectedItem.Playlist || item is SelectedItem.Folder) {
                            Option(
                                label = "Rename",
                                icon = Icons.Default.Edit,
                                onClick = onRename
                            )
                        }
                        Option(
                            label = "Move",
                            icon = Icons.AutoMirrored.Default.DriveFileMove,
                            onClick = onMove
                        )
                        Option(
                            label = "Add to playlist",
                            icon = Icons.AutoMirrored.Default.PlaylistAdd,
                            onClick = onAddToPlaylist
                        )
                        Option(
                            label = "Tag",
                            icon = Icons.Default.Tag,
                            onClick = onTag
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueue
                        )
                        Option(
                            label = "Delete",
                            icon = Icons.Default.Delete,
                            onClick = onDelete,
                        )
                        Box(Modifier)
                    }
                }
            }
        }

        @Composable
        fun MoveDialog(
            moving: StateFlow<Boolean>,
            path: StateFlow<List<Folder?>>,
            searchQuery: StateFlow<String>,
            folders: StateFlow<List<Folder>>,
            loadingFolders: StateFlow<Boolean>,
            onSearchQueryChange: (String) -> Unit,
            onNewFolder: (name: String) -> Unit,
            onFolderClick: (FolderId?) -> Unit,
            onMove: () -> Unit,
            onDismiss: () -> Unit,
        ) {
            val moving by moving.collectAsState()
            val path by path.collectAsState()
            val folders by folders.collectAsState()
            val loadingFolders by loadingFolders.collectAsState()
            val lazyColumnState = rememberLazyListState()

            var newFolderFormVisible by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier.size(width = 500.dp, height = 600.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (moving) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                        ) {
                            CircularProgressIndicator()
                            Text(text = "Please wait...")
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                    text = "Move to Folder",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )
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
                                if (loadingFolders) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                        state = lazyColumnState,
                                        contentPadding = PaddingValues(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = folders,
                                            key = { it.id.value }
                                        ) { folder ->
                                            Item(
                                                onClick = { onFolderClick(folder.id) },
                                                contentPadding = PaddingValues(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Image(
                                                        imageVector = Icons.Default.Folder,
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
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                content = { Text("Move here") },
                                onClick = onMove
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

        @Composable
        private fun TagBatchDialog(
            searchQuery: StateFlow<String>,
            tags: StateFlow<List<State.DialogState.Tag>>,
            onSearchQueryChange: (String) -> Unit,
            onAdd: () -> Unit,
            onTagClick: (TagId) -> Unit,
            onDone: (unset: Boolean) -> Unit,
            onDismiss: () -> Unit
        ) {
            val searchQuery by searchQuery.collectAsState()
            val tags by tags.collectAsState()
            val scrollState = rememberScrollState()

            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier.size(width = 500.dp, height = 600.dp),
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
                            text = "Select Tags",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search") },
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            trailingIcon = {
                                IconButton(
                                    onClick = onAdd,
                                    content = { Icon(Icons.Default.Add, "Add new tag") }
                                )
                            },
                            singleLine = true
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                for (tag in tags) {
                                    FilterChip(
                                        leadingIcon = {
                                            if (tag.isApplied) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        },
                                        label = { Text(tag.name) },
                                        selected = tag.isApplied,
                                        onClick = { onTagClick(tag.id) }
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                content = { Text("Set selected") },
                                onClick = { onDone(false) }
                            )
                            Button(
                                content = { Text("Unset selected") },
                                onClick = { onDone(true) }
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun TagTrackDialog(
            searchQuery: StateFlow<String>,
            item: State.Content.Loaded.Track,
            tags: StateFlow<List<State.DialogState.Tag>>,
            onSearchQueryChange: (String) -> Unit,
            onAdd: () -> Unit,
            onTagClick: (State.DialogState.Tag) -> Unit,
            onDismiss: () -> Unit
        ) {
            val searchQuery by searchQuery.collectAsState()
            val tags by tags.collectAsState()
            val scrollState = rememberScrollState()

            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier.size(width = 500.dp, height = 600.dp),
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
                            text = "Select Tags",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(64.dp),
                                id = item.image
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search") },
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            trailingIcon = {
                                IconButton(
                                    onClick = onAdd,
                                    content = { Icon(Icons.Default.Add, "Add new tag") }
                                )
                            },
                            singleLine = true
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                for (tag in tags) {
                                    FilterChip(
                                        leadingIcon = {
                                            if (tag.isApplied) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        },
                                        label = { Text(tag.name) },
                                        selected = tag.isApplied,
                                        onClick = { onTagClick(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun FolderItem(
            modifier: Modifier,
            folder: Folder,
            isSelected: Boolean,
            onClick: () -> Unit,
            onToggleSelection: () -> Unit,
            onPlayClick: () -> Unit,
            onOptionsClick: () -> Unit
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val windowSizeClass = getWindowSizeClass()

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
                        if (windowSizeClass != WindowWidthSizeClass.Compact) {
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
                                    onClick = onOptionsClick
                                )
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

        @Composable
        private fun PlaylistItem(
            modifier: Modifier,
            playlist: State.Content.Loaded.Playlist,
            isSelected: Boolean,
            onClick: () -> Unit,
            onToggleSelection: () -> Unit,
            onPlayClick: () -> Unit,
            onOptionsClick: () -> Unit
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val windowSizeClass = getWindowSizeClass()

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
                        modifier = Modifier.fillMaxSize(),
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
                        if (windowSizeClass != WindowWidthSizeClass.Compact) {
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
                                    onClick = onOptionsClick
                                )
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

        @Composable
        private fun TrackItem(
            modifier: Modifier,
            track: State.Content.Loaded.Track,
            isSelected: Boolean,
            onClick: () -> Unit,
            onToggleSelection: () -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onOptionsClick: () -> Unit
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
                        modifier = Modifier.fillMaxSize(),
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
                        AdaptiveUi(
                            wide = {
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
                                        onClick = onOptionsClick
                                    )
                                }
                            },
                            compact = {
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
                        )
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
