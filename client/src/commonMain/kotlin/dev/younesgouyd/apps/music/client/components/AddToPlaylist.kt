package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.Image
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
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.Track
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AddToPlaylist(
    private val itemToAdd: Item,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val artistRepo: ArtistRepo,
    private val mediaFileRepo: MediaFileRepo,
    private val dismiss: () -> Unit,
    private val playlistRepo: PlaylistRepo
) : Component() {
    override val title: String = "Add to Playlist"
    private val state: MutableStateFlow<AddToPlaylistState> = MutableStateFlow(AddToPlaylistState.Loading)
    private val _adding: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val adding: StateFlow<Boolean> get() = _adding.asStateFlow()

    init {
        coroutineScope.launch {
            state.update {
                AddToPlaylistState.Loaded(
                    adding = _adding.asStateFlow(),
                    itemToAdd = when (itemToAdd) {
                        is Item.Track -> trackRepo.get(itemToAdd.id).first().let { dbTrack ->
                            AddToPlaylistState.Loaded.ItemToAdd(
                                name = dbTrack.name,
                                image = mediaFileRepo.getTrackImage(dbTrack.id)
                            )
                        }
                        is Item.Playlist -> playlistRepo.get(itemToAdd.id).first().let { dbPlaylist ->
                            AddToPlaylistState.Loaded.ItemToAdd(
                                name = dbPlaylist.name,
                                image = mediaFileRepo.getPlaylistImage(dbPlaylist.id)
                            )
                        }
                        is Item.Folder -> folderRepo.get(itemToAdd.id).first().let { dbFolder ->
                            AddToPlaylistState.Loaded.ItemToAdd(
                                name = dbFolder.name,
                                image = null
                            )
                        }
                        is Item.Artist -> artistRepo.get(itemToAdd.id).first().let { dbArtist ->
                            AddToPlaylistState.Loaded.ItemToAdd(
                                name = dbArtist.name,
                                image = mediaFileRepo.getArtistImage(dbArtist.id)
                            )
                        }
                    },
                    playlists = playlistRepo.getAll().map { list ->
                        list.map { dbPlaylist ->
                            AddToPlaylistState.Loaded.PlaylistOption(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = mediaFileRepo.getPlaylistImage(dbPlaylist.id)
                            )
                        }
                    }.stateIn(coroutineScope),
                    onAddTopPlaylist = ::addToPlaylist
                )
            }
        }
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

    private fun addToPlaylist(playlistToAddTo: AddToPlaylistState.Loaded.PlaylistToAddTo) {
        coroutineScope.launch {
            _adding.update { true }
            val playlistId = when (playlistToAddTo) {
                is AddToPlaylistState.Loaded.PlaylistToAddTo.Id -> playlistToAddTo.value
                is AddToPlaylistState.Loaded.PlaylistToAddTo.New -> playlistRepo.add(
                    name = playlistToAddTo.name,
                    folderId = null,
                    importSessionId = null,
                    importUri = null
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
                    val tracks: List<Track> = trackRepo.getPlaylistTracks(itemToAdd.id).first()
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
                    val tracks: List<Track> = trackRepo.getArtistTracks(itemToAdd.id).first()
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

    sealed class Item {
        data class Track(val id: TrackId) : Item()

        data class Playlist(val id: PlaylistId) : Item()

        data class Folder(val id: FolderId) : Item()

        data class Artist(val id: ArtistId) : Item()
    }

    private sealed class AddToPlaylistState {
        data object Loading : AddToPlaylistState()

        data class Loaded(
            val adding: StateFlow<Boolean>,
            val itemToAdd: ItemToAdd,
            val playlists: StateFlow<List<PlaylistOption>>,
            val onAddTopPlaylist: (playlistId: PlaylistToAddTo) -> Unit
        ) : AddToPlaylistState() {
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
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: AddToPlaylistState
        ) {
            when (state) {
                is AddToPlaylistState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AddToPlaylistState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        fun Main(
            modifier: Modifier,
            loaded: AddToPlaylistState.Loaded
        ) {
            val adding by loaded.adding.collectAsState()
            val itemToAdd = loaded.itemToAdd
            val playlists by loaded.playlists.collectAsState()
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
                            dev.younesgouyd.apps.music.client.components.util.compose.widgets.Image(
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

                                    Item(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
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
                                                    loaded.onAddTopPlaylist(
                                                         AddToPlaylistState.Loaded.PlaylistToAddTo.New(name)
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
                                    Item(
                                        modifier = Modifier.padding(8.dp),
                                        onClick = {
                                            loaded.onAddTopPlaylist(
                                                AddToPlaylistState.Loaded.PlaylistToAddTo.Id(
                                                    playlistOption.id
                                                )
                                            )
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dev.younesgouyd.apps.music.client.components.util.compose.widgets.Image(
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
                            }
                        }
                    }
                }
            } else {
                Text(modifier = modifier, text = "Adding...")
            }
        }
    }
}