package dev.younesgouyd.apps.music.client.app.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Image
import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Track
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import dev.younesgouyd.apps.music.client.app.multiplatform.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Offset
import dev.younesgouyd.apps.music.client.app.multiplatform.util.PageSize
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AddToPlaylist(
    itemToAdd: Item,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    artistRepo: SpotifyArtistRepo,
    albumRepo: SpotifyAlbumRepo,
    playlistRepo: PlaylistRepo,
    mediaFileRepo: MediaFileRepo,
    dismiss: () -> Unit
) : Component() {
    override val title: String = "Add to Playlist"
    private val state: StateFlow<Ui.State>
    private val _adding: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val adding = _adding.asStateFlow()

    init {
        val item = when (itemToAdd) {
            is Item.Track -> trackRepo.get(itemToAdd.id).filterNotNull().map { dbTrack ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbTrack.spotifyTrack?.name ?: dbTrack.originalImport.title,
                    image = if (dbTrack.spotifyTrack != null) {
                        mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack.spotifyAlbumId)
                    } else {
                        mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
                    }
                )
            }
            is Item.Playlist -> playlistRepo.get(itemToAdd.id).filterNotNull().map { dbPlaylist ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbPlaylist.name,
                    image = null
                )
            }
            is Item.Folder -> folderRepo.get(itemToAdd.id).filterNotNull().map { dbFolder ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbFolder.name,
                    image = null
                )
            }
            is Item.Artist -> artistRepo.get(itemToAdd.id).filterNotNull().map { dbArtist ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbArtist.name,
                    image = mediaFileRepo.getSpotifyArtistImage(itemToAdd.id)
                )
            }
            is Item.Album -> albumRepo.get(itemToAdd.id).filterNotNull().map { dbAlbum ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbAlbum.name,
                    image = mediaFileRepo.getSpotifyAlbumImage(itemToAdd.id)
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        var loaded: Ui.State.Loaded? = null
        state = item.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    adding = _adding.asStateFlow(),
                    itemToAdd = item.filterNotNull().stateIn(coroutineScope),
                    playlists = LazilyLoadedItems(
                        coroutineScope = coroutineScope,
                        load = { offset: Offset.Id<PlaylistId>, pageSize: PageSize ->
                            val rows = playlistRepo.getAll(pageSize, offset)
                            LazilyLoadedItems.Page(
                                nextOffset = rows.lastOrNull()?.id?.let {
                                    Offset.Id(
                                        it
                                    )
                                },
                                items = rows.map { dbPlaylist ->
                                    Ui.State.Loaded.PlaylistOption(
                                        id = dbPlaylist.id,
                                        name = dbPlaylist.name,
                                        image = null
                                    )
                                }
                            )
                        },
                        initialOffset = Offset.Id.initial<PlaylistId>()
                    ),
                    onDoneClick =  { playlistToAddTo: Ui.State.Loaded.PlaylistToAddTo ->
                        coroutineScope.launch {
                            _adding.update { true }
                            val playlistId = when (playlistToAddTo) {
                                is Ui.State.Loaded.PlaylistToAddTo.Id -> playlistToAddTo.value
                                is Ui.State.Loaded.PlaylistToAddTo.New -> playlistRepo.add(
                                    name = playlistToAddTo.name,
                                    folderId = null
                                )
                            }
                            when (itemToAdd) {
                                is Item.Track -> {
                                    val exists = playlistTrackCrossRefRepo.get(playlistId, itemToAdd.id).first() != null
                                    if (!exists) {
                                        playlistTrackCrossRefRepo.add(playlistId, itemToAdd.id)
                                    }
                                }
                                is Item.Playlist -> {
                                    val tracks: List<Track> = trackRepo.getPlaylistTracks(itemToAdd.id).first().map { it.track }
                                    for (track in tracks) {
                                        val exists = playlistTrackCrossRefRepo.get(playlistId, track.id).first() != null
                                        if (!exists) {
                                            playlistTrackCrossRefRepo.add(playlistId, track.id)
                                        }
                                    }
                                }
                                is Item.Folder -> {
                                    suspend fun addFolderToPlaylist(folderId: FolderId) {
                                        val tracks: List<Track> = trackRepo.getFolderTracks(folderId).first()
                                        for (track in tracks) {
                                            val exists = playlistTrackCrossRefRepo.get(playlistId, track.id).first() != null
                                            if (!exists) {
                                                playlistTrackCrossRefRepo.add(playlistId, track.id)
                                            }
                                        }
                                        val subfolders = folderRepo.getSubfolders(folderId).first()
                                        for (subfolder in subfolders) {
                                            addFolderToPlaylist(subfolder.id)
                                        }
                                    }
                                    addFolderToPlaylist(itemToAdd.id)
                                }
                                is Item.Artist -> {
                                    val tracks: List<Track> = trackRepo.getArtistTracks(itemToAdd.id).first().map { it.track }
                                    for (track in tracks) {
                                        val exists = playlistTrackCrossRefRepo.get(playlistId, track.id).first() != null
                                        if (!exists) {
                                            playlistTrackCrossRefRepo.add(playlistId, track.id)
                                        }
                                    }
                                }
                                is Item.Album -> {
                                    val tracks: List<Track> = trackRepo.getAlbumTracks(itemToAdd.id).first().map { it.track }
                                    for (track in tracks) {
                                        val exists = playlistTrackCrossRefRepo.get(playlistId, track.id).first() != null
                                        if (!exists) {
                                            playlistTrackCrossRefRepo.add(playlistId, track.id)
                                        }
                                    }
                                }
                            }
                            _adding.update { false }
                            dismiss()
                        }
                    }
                )
            }
        }.map {
            if (it == null) {
                Ui.State.ItemDoesNotExist
            } else {
                loaded!!
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Loading)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(
            modifier = modifier.size(width = 500.dp, height = 600.dp),
            state = state
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    sealed class Item {
        data class Track(val id: TrackId) : Item()

        data class Playlist(val id: PlaylistId) : Item()

        data class Folder(val id: FolderId) : Item()

        data class Artist(val id: SpotifyArtistId) : Item()

        data class Album(val id: SpotifyAlbumId) : Item()
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val adding: StateFlow<Boolean>,
                val itemToAdd: StateFlow<ItemToAdd>,
                val playlists: LazilyLoadedItems<PlaylistOption, Offset.Id<PlaylistId>>,
                val onDoneClick: (playlistId: PlaylistToAddTo) -> Unit
            ) : State() {
                data class ItemToAdd(
                    val name: String,
                    val image: File?
                )

                data class PlaylistOption(
                    val id: PlaylistId,
                    val name: String,
                    val image: File?
                )

                sealed class PlaylistToAddTo {
                    data class Id(val value: PlaylistId) : PlaylistToAddTo()

                    data class New(val name: String) : PlaylistToAddTo()
                }
            }

            data object ItemDoesNotExist : State()
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, loaded = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        fun Main(
            modifier: Modifier,
            loaded: State.Loaded
        ) {
            val adding by loaded.adding.collectAsState()
            val itemToAdd by loaded.itemToAdd.collectAsState()
            val playlists by loaded.playlists.items.collectAsState()
            val loadingItems by loaded.playlists.loading.collectAsState()
            val lazyColumnState = rememberLazyListState()

            if (!adding) {
                Surface(
                    modifier = modifier,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            text = "Add to playlist",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(64.dp),
                                file = itemToAdd.image
                            )
                            Text(
                                text = itemToAdd.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                state = lazyColumnState,
                                contentPadding = PaddingValues(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    var name by remember { mutableStateOf("") }

                                    dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Item(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.foundation.Image(
                                                modifier = Modifier.size(64.dp),
                                                imageVector = Icons.AutoMirrored.Default.PlaylistAdd,
                                                contentDescription = null
                                            )
                                            OutlinedTextField(
                                                modifier = Modifier.weight(1f),
                                                label = { Text("Create new playlist") },
                                                value = name,
                                                onValueChange = { name = it }
                                            )
                                            Button(
                                                content = { Text("Done") },
                                                onClick = {
                                                    loaded.onDoneClick(
                                                        State.Loaded.PlaylistToAddTo.New(name)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = playlists,
                                    key = { it.id.value }
                                ) { playlistOption ->
                                    dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Item(
                                        modifier = Modifier.padding(8.dp),
                                        onClick = {
                                            loaded.onDoneClick(
                                                State.Loaded.PlaylistToAddTo.Id(playlistOption.id)
                                            )
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                modifier = Modifier.size(64.dp),
                                                file = playlistOption.image
                                            )
                                            Text(
                                                text = playlistOption.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (loadingItems) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(lazyColumnState) {
                    snapshotFlow {
                        lazyColumnState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    }.map { it == null ||  it >= (playlists.size + 1) - 5  }
                        .filter { it }
                        .collect { loaded.playlists.loadMore() }
                }
            } else {
                Text(modifier = modifier, text = "Adding...")
            }
        }
    }
}