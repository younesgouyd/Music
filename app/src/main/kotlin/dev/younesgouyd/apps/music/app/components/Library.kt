package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.components.util.widgets.*
import dev.younesgouyd.apps.music.app.data.repoes.*
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Folder
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Playlist
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

class Library(
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    private val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    private val artistTrackCrossRefRepo: ArtistTrackCrossRefRepo,
    private val showPlaylist: (id: Long) -> Unit,
    private val playTrack: (id: Long) -> Unit,
    private val showArtistDetails: (id: Long) -> Unit,
    private val addTrackToQueue: (id: Long) -> Unit,
    private val playQueue: (List<MediaController.QueueItemParameter>) -> Unit
) : Component() {
    override val title: String = "Library"
    private val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    private val path: StateFlow<Set<Folder>>
    private val folders: StateFlow<List<Folder>>
    private val playlists: StateFlow<List<Playlist>>
    private val tracks: StateFlow<List<State.Track>>
    private val loadingItems: StateFlow<Boolean>
    private val loadingFolders: MutableStateFlow<Boolean>
    private val loadingPlaylists: MutableStateFlow<Boolean>
    private val loadingTracks: MutableStateFlow<Boolean>
    private val importingFolder: MutableStateFlow<Boolean>

    init {
        loadingFolders = MutableStateFlow(true)
        loadingPlaylists = MutableStateFlow(true)
        loadingTracks = MutableStateFlow(true)
        importingFolder = MutableStateFlow(false)
        loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks, importingFolder) { loading1, loading2, loading3, loading4 ->
            loading1 || loading2 || loading3 || loading4
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
                        emit(
                            tracks.map { dbTrack ->
                                State.Track(
                                    id = dbTrack.id,
                                    name = dbTrack.name,
                                    audioUrl = dbTrack.audio_url,
                                    videoUrl = dbTrack.video_url,
                                    artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                        State.Track.Artist(
                                            id = dbArtist.id,
                                            name = dbArtist.name
                                        )
                                    },
                                    album = dbTrack.album_id?.let {
                                        albumRepo.getStatic(it).let { dbAlbum ->
                                            State.Track.Album(
                                                id = dbAlbum.id,
                                                name = dbAlbum.name,
                                                image = dbAlbum.image
                                            )
                                        }
                                    }
                                )
                            }
                        )
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
            modifier = modifier,
            path = path,
            loadingItems = loadingItems,
            currentFolder = currentFolder.asStateFlow(),
            folders = folders,
            playlists = playlists,
            tracks = tracks,
            onImportFolder = ::importFolder,
            onNewFolder = ::addFolder,
            onNewTrack = ::addTrack,
            onFolderClick = ::openFolder,
            onPlayFolder = ::playFolder,
            onPlaylistClick = showPlaylist,
            onTrackClick = playTrack,
            onArtistClick = showArtistDetails,
            onRenameFolder = ::renameFolder,
            onRenamePlaylist = ::renamePlaylist,
            onDeleteFolder = ::deleteFolder,
            onDeletePlaylist = ::deletePlaylist,
            onDeleteTrack = ::deleteTrack,
            onAddTrackToQueue = addTrackToQueue
        )
    }

    override fun clear() {
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
                videoUrl = videoUrl,
                lyrics = null,
                albumTrackNumber = null
            )
        }
    }

    private fun playFolder(folderId: Long) {
        suspend fun getFolderItems(_folderId: Long): List<MediaController.QueueItemParameter> {
            val tracks = trackRepo.getFolderTracksStatic(_folderId).map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
            val playlists = playlistRepo.getFolderPlaylistsStatic(_folderId).map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
            val albums = albumRepo.getFolderAlbumsStatic(_folderId).map { dbAlbum -> MediaController.QueueItemParameter.Album(dbAlbum.id) }
            return tracks + playlists + albums + folderRepo.getSubfoldersStatic(_folderId).flatMap { getFolderItems(it.id) }
        }
        coroutineScope.launch {
            val queue = getFolderItems(folderId)
            playQueue(queue)
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

    private fun importFolder(path: String) {
        suspend fun importFolder(folder: File, parent: Long?) {
            val parent: Long = folderRepo.add(folder.name, parent)
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    importFolder(file, parent)
                } else if (file.extension.lowercase() == "mp3") {
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
                        folderId = parent,
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
            }
        }
        coroutineScope.launch {
            val main = File(path)
            importingFolder.value = true
            importFolder(main, null)
            importingFolder.value = false
        }
    }

    private object State {
        data class Track(
            val id: Long,
            val name: String,
            val audioUrl: String?,
            val videoUrl: String?,
            val artists: List<Artist>,
            val album: Album?
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

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier = Modifier,
            path: StateFlow<Set<Folder>>,
            loadingItems: StateFlow<Boolean>,
            currentFolder: StateFlow<Folder?>,
            folders: StateFlow<List<Folder>>,
            playlists: StateFlow<List<Playlist>>,
            tracks: StateFlow<List<State.Track>>,
            onImportFolder: (path: String) -> Unit,
            onNewFolder: (name: String) -> Unit,
            onNewTrack: (name: String, audioUrl: String?, videoUrl: String?) -> Unit,
            onFolderClick: (Folder?) -> Unit,
            onPlayFolder: (id: Long) -> Unit,
            onPlaylistClick: (id: Long) -> Unit,
            onTrackClick: (id: Long) -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onRenameFolder: (Long, name: String) -> Unit,
            onRenamePlaylist: (id: Long, name: String) -> Unit,
            onDeleteFolder: (id: Long) -> Unit,
            onDeletePlaylist: (id: Long) -> Unit,
            onDeleteTrack: (id: Long) -> Unit,
            onAddTrackToQueue: (id: Long) -> Unit
        ) {
            val path by path.collectAsState()
            val loadingItems by loadingItems.collectAsState()
            val folders by folders.collectAsState()
            val playlists by playlists.collectAsState()
            val tracks by tracks.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Column(
                modifier = modifier,
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
                                onImportFolder = onImportFolder,
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
                                            onPlay = { onPlayFolder(folder.id) },
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
                                            onArtistClick = onArtistClick,
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
            onPlay: () -> Unit,
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
                        horizontalArrangement = Arrangement.End,
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
                        IconButton(
                            onClick = onPlay,
                            content = { Icon(Icons.Default.PlayCircle, null) }
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
                        horizontalArrangement = Arrangement.End,
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
            track: State.Track,
            onClick: () -> Unit,
            onArtistClick: (id: Long) -> Unit,
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
                        data = track.album?.image, // TODO
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
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "By: ",
                            style = MaterialTheme.typography.titleMedium
                        )
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
                        horizontalArrangement = Arrangement.End,
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
}
