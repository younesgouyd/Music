package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.compose.AdaptiveUi
import dev.younesgouyd.apps.music.client.components.util.compose.formatted
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Image
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.Artist
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.data.room.entities.Playlist
import dev.younesgouyd.apps.music.client.data.room.entities.Tag
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class TrackDetails(
    private val id: TrackId,
    trackRepo: TrackRepo,
    tagRepo: TagRepo,
    importSessionItemRepo: ImportSessionItemRepo,
    artistRepo: ArtistRepo,
    playlistRepo: PlaylistRepo,
    tagTrackCrossRefRepo: TagTrackCrossRefRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    mediaFileRepo: MediaFileRepo,
    showArtist: (ArtistId) -> Unit,
    showTag: (TagId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showImportSessionItem: (ImportSessionItemId) -> Unit
) : Component() {
    override val title: String = "Track"
    private val selectedTab = MutableStateFlow(TrackDetailsState.Loaded.Tab.Track)
    private val state: MutableStateFlow<TrackDetailsState> = MutableStateFlow(TrackDetailsState.Loading)

    init {
        coroutineScope.launch {
            state.value = TrackDetailsState.Loaded(
                selectedTab = selectedTab.asStateFlow(),
                track = trackRepo.get(id).map { dbTrack ->
                    TrackDetailsState.Loaded.Track(
                        id = dbTrack.id,
                        name = dbTrack.name,
                        image = mediaFileRepo.getTrackImage(dbTrack.id),
                        album = dbTrack.album,
                        lyrics = dbTrack.lyrics,
                        duration = dbTrack.duration
                    )
                }.stateIn(coroutineScope),
                artists = artistRepo.getTrackArtists(id).stateIn(coroutineScope),
                appliedTags = tagRepo.getTrackTags(id).stateIn(coroutineScope),
                unappliedTags = tagRepo.getTrackUnsetTags(id).stateIn(coroutineScope),
                imports = importSessionItemRepo.getTrackImports(id).stateIn(coroutineScope),
                playlists = playlistRepo.getTrackPlaylists(id).stateIn(coroutineScope),
                onTabClick = { selectedTab.value = it },
                onArtistClick = showArtist,
                onSetTag = { coroutineScope.launch { tagTrackCrossRefRepo.add(it, id) } },
                onUnsetTag = { coroutineScope.launch { tagTrackCrossRefRepo.delete(it, id) } },
                onTagDetailsClick = showTag,
                onImportClick = showImportSessionItem,
                onPlaylistClick = showPlaylist,
                onRemoveFromPlaylistClick = { coroutineScope.launch { playlistTrackCrossRefRepo.delete(it, id) } }
            )
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class TrackDetailsState {
        data object Loading : TrackDetailsState()

        data class Loaded(
            val selectedTab: StateFlow<Tab>,
            val track: StateFlow<Track>,
            val artists: StateFlow<List<Artist>>,
            val appliedTags: StateFlow<List<Tag>>,
            val unappliedTags: StateFlow<List<Tag>>,
            val imports: StateFlow<List<ImportSessionItem>>,
            val playlists: StateFlow<List<Playlist>>,
            val onTabClick: (Tab) -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onSetTag: (TagId) -> Unit,
            val onUnsetTag: (TagId) -> Unit,
            val onTagDetailsClick: (TagId) -> Unit,
            val onImportClick: (ImportSessionItemId) -> Unit,
            val onPlaylistClick: (PlaylistId) -> Unit,
            val onRemoveFromPlaylistClick: (PlaylistId) -> Unit
        ) : TrackDetailsState() {
            enum class Tab {
                Track, Tags, Artists, Imports, Playlists
            }
            
            data class Track(
                val id: TrackId,
                val name: String,
                val image: File?,
                val album: String?,
                val lyrics: String?,
                val duration: Duration?
            )
        }
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: TrackDetailsState
        ) {
            when (state) {
                is TrackDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is TrackDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            state: TrackDetailsState.Loaded
        ) {
            Main(
                modifier = modifier,
                selectedTab = state.selectedTab,
                track = state.track,
                artists = state.artists,
                appliedTags = state.appliedTags,
                unappliedTags = state.unappliedTags,
                imports = state.imports,
                playlists = state.playlists,
                onTabClick = state.onTabClick,
                onArtistClick = state.onArtistClick,
                onSetTag = state.onSetTag,
                onUnsetTag = state.onUnsetTag,
                onTagDetailsClick = state.onTagDetailsClick,
                onImportClick = state.onImportClick,
                onPlaylistClick = state.onPlaylistClick,
                onRemoveFromPlaylistClick = state.onRemoveFromPlaylistClick
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            selectedTab: StateFlow<TrackDetailsState.Loaded.Tab>,
            track: StateFlow<TrackDetailsState.Loaded.Track>,
            artists: StateFlow<List<Artist>>,
            appliedTags: StateFlow<List<Tag>>,
            unappliedTags: StateFlow<List<Tag>>,
            imports: StateFlow<List<ImportSessionItem>>,
            playlists: StateFlow<List<Playlist>>,
            onTabClick: (TrackDetailsState.Loaded.Tab) -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onSetTag: (TagId) -> Unit,
            onUnsetTag: (TagId) -> Unit,
            onTagDetailsClick: (TagId) -> Unit,
            onImportClick: (ImportSessionItemId) -> Unit,
            onPlaylistClick: (PlaylistId) -> Unit,
            onRemoveFromPlaylistClick: (PlaylistId) -> Unit
        ) {
            val selectedTab by selectedTab.collectAsState()
            val track by track.collectAsState()
            val artists by artists.collectAsState()
            val appliedTags by appliedTags.collectAsState()
            val unappliedTags by unappliedTags.collectAsState()
            val imports by imports.collectAsState()
            val playlists by playlists.collectAsState()

            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryScrollableTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = TrackDetailsState.Loaded.Tab.entries.indexOf(selectedTab)
                    ) {
                        for (tab in TrackDetailsState.Loaded.Tab.entries) {
                            Tab(
                                text = { Text(tab.name) },
                                selected = false,
                                onClick = { onTabClick(tab) }
                            )
                        }
                    }
                    when (selectedTab) {
                        TrackDetailsState.Loaded.Tab.Track -> {
                            TrackInfo(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                track = track
                            )
                        }
                        TrackDetailsState.Loaded.Tab.Tags -> Tags(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            applied = appliedTags,
                            unapplied = unappliedTags,
                            onSetTag = onSetTag,
                            onUnsetTag = onUnsetTag,
                            onTagDetailsClick = onTagDetailsClick
                        )
                        TrackDetailsState.Loaded.Tab.Artists -> Artists(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            artists = artists,
                            onArtistClick = onArtistClick
                        )
                        TrackDetailsState.Loaded.Tab.Imports -> Imports(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            imports = imports,
                            onImportClick = onImportClick
                        )
                        TrackDetailsState.Loaded.Tab.Playlists -> Playlists(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            playlists = playlists,
                            onPlaylistClick = onPlaylistClick,
                            onRemoveFromPlaylistClick = onRemoveFromPlaylistClick
                        )
                    }
                }
            }
        }

        @Composable
        private fun TrackInfo(
            modifier: Modifier,
            track: TrackDetailsState.Loaded.Track
        ) {
            Surface(modifier = modifier) {
                AdaptiveUi(
                    wide = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.fillMaxHeight(),
                                file = track.image
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.displayMedium
                                )
                                if (track.album != null) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Album, null)
                                            Text(
                                                text = track.album,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (track.duration != null) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Timer, null)
                                            Text(
                                                text = track.duration.formatted(),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    compact = {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                modifier = Modifier.fillMaxWidth(),
                                file = track.image
                            )
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.displayMedium
                            )
                            if (track.album != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Album, null)
                                        Text(
                                            text = track.album,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (track.duration != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Timer, null)
                                        Text(
                                            text = track.duration.formatted(),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        @Composable
        private fun Artists(
            modifier: Modifier,
            artists: List<Artist>,
            onArtistClick: (ArtistId) -> Unit
        ) {
            LazyColumn(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(items = artists, key = { it.id.value }) { artist ->
                    Item(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onArtistClick(artist.id) }
                    ) {
                        Text(artist.name)
                    }
                }
            }
        }

        @Composable
        private fun Imports(
            modifier: Modifier,
            imports: List<ImportSessionItem>,
            onImportClick: (ImportSessionItemId) -> Unit
        ) {
            LazyColumn(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(items = imports, key = { it.id.value }) { import ->
                    Item(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onImportClick(import.id) }
                    ) {
                        Text(import.uri)
                    }
                }
            }
        }

        @Composable
        private fun Playlists(
            modifier: Modifier,
            playlists: List<Playlist>,
            onPlaylistClick: (PlaylistId) -> Unit,
            onRemoveFromPlaylistClick: (PlaylistId) -> Unit
        ) {
            LazyColumn(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(items = playlists, key = { it.id.value }) { playlist ->
                    Item(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlaylistClick(playlist.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(playlist.name)
                            IconButton(
                                onClick = { onRemoveFromPlaylistClick(playlist.id) },
                                content = { Icon(Icons.Default.Remove, null) }
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun Tags(
            modifier: Modifier,
            applied: List<Tag>,
            unapplied: List<Tag>,
            onSetTag: (TagId) -> Unit,
            onUnsetTag: (TagId) -> Unit,
            onTagDetailsClick: (TagId) -> Unit
        ) {
            Column(
                modifier = modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    for (tag in applied) {
                        Item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = { onUnsetTag(tag.id) }
                                )
                                Text(tag.name)
                                IconButton(
                                    content = { Icon(Icons.Default.Info, null) },
                                    onClick = { onTagDetailsClick(tag.id) }
                                )
                            }
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    for (tag in unapplied) {
                        Item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { onSetTag(tag.id) }
                                )
                                Text(tag.name)
                                IconButton(
                                    content = { Icon(Icons.Default.Info, null) },
                                    onClick = { onTagDetailsClick(tag.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}