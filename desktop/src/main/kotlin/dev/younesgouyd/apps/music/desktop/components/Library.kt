package dev.younesgouyd.apps.music.desktop.components

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
import com.mpatric.mp3agic.Mp3File
import dev.younesgouyd.apps.music.common.components.Library
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Folder
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Playlist
import dev.younesgouyd.apps.music.desktop.components.util.widgets.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

class Library(
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    albumRepo: AlbumRepo,
    artistRepo: ArtistRepo,
    artistTrackCrossRefRepo: ArtistTrackCrossRefRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val mediaController: MediaController,
    private val showPlaylist: (id: Long) -> Unit,
    private val showArtistDetails: (id: Long) -> Unit
) : Library(
    folderRepo, playlistRepo, trackRepo, albumRepo, artistRepo, artistTrackCrossRefRepo,
    playlistTrackCrossRefRepo, mediaController
) {
    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(
            modifier = modifier,
            path = path,
            loadingItems = loadingItems,
            currentFolder = currentFolder.asStateFlow(),
            folders = folders,
            playlists = playlists,
            tracks = tracks,
            addToPlaylistDialogVisible = addToPlaylistDialogVisible,
            addToPlaylist = addToPlaylist.asStateFlow(),
            onImportFolder = ::importFolder,
            onNewFolder = {
                coroutineScope.launch {
                    folderRepo.add(
                        name = it,
                        parentFolderId = currentFolder.value?.id
                    )
                }
            },
            onNewTrack = ::addTrack,
            onFolderClick = { currentFolder.value = it },
            onAddFolderToPlaylistClick = ::showAddFolderToPlaylistDialog,
            onAddFolderToQueueClick = ::addFolderToQueue,
            onPlayFolder = ::playFolder,
            onPlaylistClick = showPlaylist,
            onPlayPlaylistClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(it))) },
            onAddPlaylistToPlaylistClick = ::showAddPlaylistToPlaylistDialog,
            onAddPlaylistToQueueClick = {
                mediaController.addToQueue(
                    listOf(
                        MediaController.QueueItemParameter.Playlist(
                            it
                        )
                    )
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
            loadFolders = folderRepo::getSubfoldersStatic,
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

    suspend fun saveMp3FileAsTrack(file: File, parentFolderId: Long) {
        val mp3file = Mp3File(file)
        var title: String? = null
        var albumTrackNumber: Long? = null
        var artist: String? = null
        var album: String? = null
        var lyrics: String? = null
        var year: String? = null
        var albumImage: ByteArray? = null
        if (mp3file.hasId3v2Tag()) {
            val id3 = mp3file.id3v2Tag
            val albumImageData = id3.albumImage
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
            lyrics = id3.lyrics
            albumImage = albumImageData
        } else if (mp3file.hasId3v1Tag()) {
            val id3 = mp3file.id3v1Tag
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
        }
        var artistId: Long? = null
        var albumId: Long? = null
        if (!artist.isNullOrEmpty()) {
            val artists = artistRepo.getByName(artist)
            if (artists.isEmpty()) {
                artistId = artistRepo.add(name = artist, image = null)
            } else if (artists.size == 1) {
                artistId = artists.first().id
            }
        }
        if (!album.isNullOrEmpty()) {
            val albums = albumRepo.getByName(album)
            if (albums.isEmpty()) {
                albumId = albumRepo.add(name = album, image = albumImage, releaseDate = year)
            } else if (albums.size == 1) {
                albumId = albums.first().id
            }
        }
        val trackId = trackRepo.add(
            name = if (!title.isNullOrEmpty()) title else file.name,
            folderId = parentFolderId,
            albumId = albumId,
            audioUrl = file.toURI().toString(),
            videoUrl = null,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber
        )
        if (artistId != null) {
            artistTrackCrossRefRepo.add(artistId, trackId)
        }
    }

    override fun showAddTrackToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Track(
                    trackId
                ),
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

    override fun showAddPlaylistToPlaylistDialog(playlistId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Playlist(
                    playlistId
                ),
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

    override fun showAddFolderToPlaylistDialog(folderId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Folder(
                    folderId
                ),
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

    private fun importFolder(path: String) {
        suspend fun importFolder(folder: File, parent: Long?) {
            val parent: Long = folderRepo.add(folder.name, parent)
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    importFolder(file, parent)
                } else if (file.extension.lowercase() == "mp3") {
                    saveMp3FileAsTrack(file, parent)
                }
            }
        }
        coroutineScope.launch {
            val main = File(path)
            importingFolder.value = true
            importFolder(main, null)
            importingFolder.value = false
        }
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier = Modifier,
            path: StateFlow<Set<Folder>>,
            loadingItems: StateFlow<Boolean>,
            currentFolder: StateFlow<Folder?>,
            folders: StateFlow<List<Folder>>,
            playlists: StateFlow<List<Playlist>>,
            tracks: StateFlow<List<Models.Track>>,
            onImportFolder: (path: String) -> Unit,
            onNewFolder: (name: String) -> Unit,
            addToPlaylistDialogVisible: StateFlow<Boolean>,
            addToPlaylist: StateFlow<dev.younesgouyd.apps.music.common.components.AddToPlaylist?>,
            onNewTrack: (name: String, audioUrl: String?, videoUrl: String?) -> Unit,
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
            loadFolders: suspend (parentFolderId: Long?) -> List<Folder>,
            onMoveFolderToFolder: (id: Long, folderId: Long) -> Unit,
            onMoveTrackToFolder: (id: Long, folderId: Long) -> Unit,
            onMovePlaylistToFolder: (id: Long, folderId: Long) -> Unit
        ) {
            val path by path.collectAsState()
            val loadingItems by loadingItems.collectAsState()
            val folders by folders.collectAsState()
            val playlists by playlists.collectAsState()
            val tracks by tracks.collectAsState()
            val lazyGridState = rememberLazyGridState()
            val addToPlaylistDialogVisible by addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by addToPlaylist.collectAsState()

            Scaffold(
                modifier = modifier,
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) },
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
                            path = path,
                            onFolderClick = onFolderClick,
                            onImportFolder = onImportFolder,
                            onNewFolder = onNewFolder,
                            onNewTrack = onNewTrack
                        )
                        Box(modifier = Modifier) {
//                                VerticalScrollbar(lazyGridState) // TODO
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                state = lazyGridState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                items(folders) { folder ->
                                    FolderItem(
                                        folder = folder,
                                        onClick = { onFolderClick(folder) },
                                        onAddToPlaylistClick = { onAddFolderToPlaylistClick(folder.id) },
                                        onAddToQueueClick = { onAddFolderToQueueClick(folder.id) },
                                        onPlayClick = { onPlayFolder(folder.id) },
                                        onRenameClick = { onRenameFolder(folder.id, it) },
                                        onDeleteClick = { onDeleteFolder(folder.id) },
                                        loadFolders = loadFolders,
                                        onMoveToFolder = { onMoveFolderToFolder(folder.id, it) }
                                    )
                                }
                                items(playlists) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) },
                                        onPlayClick = { onPlayPlaylistClick(playlist.id) },
                                        onAddToPlaylistClick = { onAddPlaylistToPlaylistClick(playlist.id) },
                                        onAddToQueueClick = { onAddPlaylistToQueueClick(playlist.id) },
                                        onRenameClick = { onRenamePlaylist(playlist.id, it) },
                                        onDeleteClick = { onDeletePlaylist(playlist.id) },
                                        loadFolders = loadFolders,
                                        onMoveToFolder = { onMovePlaylistToFolder(playlist.id, it) }
                                    )
                                }
                                items(tracks) { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onDeleteClick = { onDeleteTrack(track.id) },
                                        onAddToQueueClick = { onAddTrackToQueue(track.id) },
                                        onRenameClick = { onRenameTrack(track.id, it) },
                                        loadFolders = loadFolders,
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

            if (addToPlaylistDialogVisible) {
                Dialog(onDismissRequest = onDismissAddToPlaylistDialog) {
                    addToPlaylist!!.show(Modifier)
                }
            }
        }

        @Composable
        private fun ToolBar(
            modifier: Modifier = Modifier,
            currentFolder: StateFlow<Folder?>,
            path: Set<Folder>,
            onFolderClick: (Folder?) -> Unit,
            onImportFolder: (path: String) -> Unit,
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
                            onClick = {
                                fun getFolderPathFromSystemFolderPicker(): String? {
                                    val chooser = JFileChooser().apply {
                                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                        dialogTitle = "Choose a folder"
                                    }
                                    val result = chooser.showOpenDialog(null)
                                    return if (result == JFileChooser.APPROVE_OPTION) {
                                        chooser.selectedFile.absolutePath
                                    } else {
                                        null // User canceled the selection
                                    }
                                }
                                getFolderPathFromSystemFolderPicker()?.let {
                                    onImportFolder(it)
                                }
                            },
                            content = { Icon(Icons.Default.ImportExport, null) }
                        )
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
                RenameDialog(
                    title = "New folder",
                    onDone = { onNewFolder(it); newFolderFormVisible = false },
                    onDismiss = { newFolderFormVisible = false }
                )
            }

            if (newTrackFormVisible) {
                TrackForm(
                    title = "New Track",
                    onDone = { name: String, audioUrl: String?, videoUrl: String? ->
                        onNewTrack(name, audioUrl, videoUrl)
                        newTrackFormVisible = false
                    },
                    onDismiss = { newTrackFormVisible = false }
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
            loadFolders: suspend (parentFolderId: Long?) -> List<Folder>,
            onMoveToFolder: (id: Long) -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
            var showEditFormDialog by remember { mutableStateOf(false) }
            var moveToFolderDialogVisible by remember { mutableStateOf(false) }

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
                        onClick = { moveToFolderDialogVisible = true },
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

            if (moveToFolderDialogVisible) {
                MoveToFolderDialog(
                    itemToMove = ItemToMove(
                        id = folder.id,
                        name = folder.name,
                        image = null, // TODO
                        typeLabel = "Folder"
                    ),
                    loadFolders = loadFolders,
                    onMoveToFolder = {
                        moveToFolderDialogVisible = false
                        showContextMenu = false
                        onMoveToFolder(it)
                    },
                    onDismissRequest = { moveToFolderDialogVisible = false }
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
            loadFolders: suspend (parentFolderId: Long?) -> List<Folder>,
            onMoveToFolder: (id: Long) -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
            var showEditFormDialog by remember { mutableStateOf(false) }
            var moveToFolderDialogVisible by remember { mutableStateOf(false) }

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
                        onClick = { moveToFolderDialogVisible = true },
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

            if (moveToFolderDialogVisible) {
                MoveToFolderDialog(
                    itemToMove = ItemToMove(
                        id = playlist.id,
                        name = playlist.name,
                        image = playlist.image,
                        typeLabel = "Playlist"
                    ),
                    loadFolders = loadFolders,
                    onMoveToFolder = {
                        moveToFolderDialogVisible = false
                        showContextMenu = false
                        onMoveToFolder(it)
                    },
                    onDismissRequest = { moveToFolderDialogVisible = false }
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
            loadFolders: suspend (parentFolderId: Long?) -> List<Folder>,
            onMoveToFolder: (id: Long) -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
            var showEditFormDialog by remember { mutableStateOf(false) }
            var moveToFolderDialogVisible by remember { mutableStateOf(false) }

            Item(modifier = modifier, onClick = onClick) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        data = track.album?.image,
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
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            content = { Icon(Icons.Default.PlayCircle, null) },
                            onClick = onClick
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
                    item = Item(name = track.name, image = track.album?.image),
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
                        onClick = { moveToFolderDialogVisible = true },
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

            if (moveToFolderDialogVisible) {
                MoveToFolderDialog(
                    itemToMove = ItemToMove(
                        id = track.id,
                        name = track.name,
                        image = track.album?.image,
                        typeLabel = "Track"
                    ),
                    loadFolders = loadFolders,
                    onMoveToFolder = {
                        moveToFolderDialogVisible = false
                        showContextMenu = false
                        onMoveToFolder(it)
                    },
                    onDismissRequest = { moveToFolderDialogVisible = false }
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
        private fun TrackForm(
            title: String,
            name: String = "",
            onDone: (name: String, audioUrl: String?, videoUrl: String?) -> Unit,
            onDismiss: () -> Unit
        ) {
            var name by remember { mutableStateOf(name) }
            var audioUrl: String? by remember { mutableStateOf(null) }
            var videoUrl: String? by remember { mutableStateOf(null) }

            fun getFilePathFromSystemFilePicker(): String? {
                TODO()
//                val fileDialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD)
//                fileDialog.isVisible = true
//                return fileDialog.file?.let { fileDialog.directory + it }
            }

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
        }
    }
}
