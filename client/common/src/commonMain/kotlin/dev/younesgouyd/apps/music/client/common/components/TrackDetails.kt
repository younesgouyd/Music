package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.MediaController
import dev.younesgouyd.apps.music.client.common.components.util.*
import dev.younesgouyd.apps.music.client.common.data.repoes.*
import dev.younesgouyd.apps.music.client.common.usecases.SetTrackMetadataFromSpotifyUseCase
import dev.younesgouyd.apps.music.client.common.usecases.UnsetSpotifyTrackUseCase
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.spotify.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TrackDetails(
    id: TrackId,
    trackRepo: TrackRepo,
    albumRepo: SpotifyAlbumRepo,
    tagRepo: TagRepo,
    spotifyTrackRepo: SpotifyTrackRepo,
    spotifySearchRepo: SpotifySearchRepo,
    artistRepo: SpotifyArtistRepo,
    playlistRepo: PlaylistRepo,
    tagTrackCrossRefRepo: TagTrackCrossRefRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    mediaFileRepo: MediaFileRepo,
    setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
    unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    mediaController: MediaController,
    showTag: (TagId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showImportSessionItem: (ImportSessionItemId) -> Unit,
    showTrack: (TrackId) -> Unit,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Track"
    private val selectedTab = MutableStateFlow(Ui.State.Loaded.Tab.Import)
    private val state: StateFlow<Ui.State>

    init {
        val track = trackRepo.get(id).filterNotNull().stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        // spotify track state
        val apiError = MutableStateFlow(false)
        val searching = MutableStateFlow(false)
        val settingTrack = MutableStateFlow(false)
        val name = MutableStateFlow("")
        val artist = MutableStateFlow("")
        val album = MutableStateFlow("")
        val searchResult = MutableStateFlow(emptyList<Ui.State.Loaded.SpotifyTrackState.SearchResultItem>())
        // ------------------------------------------------------------------------

        var loaded: Ui.State.Loaded? = null
        state = track.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    selectedTab = selectedTab.asStateFlow(),
                    import = track.map { it?.originalImport }.mapLatest { dbImport ->
                        dbImport?.let {
                            Ui.State.Loaded.Import(
                                id = dbImport.id,
                                title = dbImport.title,
                                uri = dbImport.uri,
                                duration = dbImport.durationMilliseconds.milliseconds,
                                artists = dbImport.inspection.artists,
                                album = dbImport.album,
                                image = mediaFileRepo.getImportSessionItemImage(dbImport.id)
                            )
                        }
                    }.stateIn(coroutineScope),
                    appliedTags = tagRepo.getTrackTags(id).stateIn(coroutineScope),
                    unappliedTags = tagRepo.getTrackUnsetTags(id).stateIn(coroutineScope),
                    spotifyTrackState = Ui.State.Loaded.SpotifyTrackState(
                        apiError = apiError.asStateFlow(),
                        spotifyTrack = track.filterNotNull().onEach { dbTrack -> // TODO (this overrides user input)
                            name.value = dbTrack.originalImport.title
                            artist.value = dbTrack.originalImport.inspection.artists.firstOrNull() ?: ""
                            album.value = dbTrack.originalImport.album ?: ""
                        }.mapLatest { dbTrack ->
                            dbTrack.spotifyTrack?.let { spotifyTrack ->
                                Ui.State.Loaded.SpotifyTrackState.SpotifyTrack(
                                    id = spotifyTrack.id,
                                    name = spotifyTrack.name,
                                    duration = spotifyTrack.durationMs?.milliseconds,
                                    album = albumRepo.get(spotifyTrack.spotifyAlbumId).first()?.let {
                                        Ui.State.Loaded.SpotifyTrackState.SpotifyTrack.Album(it.id, it.name)
                                    },
                                    artists = artistRepo.getSpotifyTrackSpotifyArtists(spotifyTrack.id).first().map { dbArtist ->
                                        Pair(dbArtist.id, dbArtist.name)
                                    },
                                    image = mediaFileRepo.getSpotifyAlbumImage(spotifyTrack.spotifyAlbumId),
                                    onAlbumClick = { showAlbum(spotifyTrack.spotifyAlbumId) },
                                    onArtistClick = showArtist,
                                    onRemoveClick = {
                                        coroutineScope.launch {
                                            unsetSpotifyTrackUseCase.execute(
                                                trackId = id,
                                                spotifyTrackId = spotifyTrack.id,
                                                spotifyAlbumId = spotifyTrack.spotifyAlbumId
                                            )
                                        }
                                    }
                                )
                            }
                        }.stateIn(coroutineScope),
                        searching = searching.asStateFlow(),
                        settingTrack = settingTrack.asStateFlow(),
                        name = name.asStateFlow(),
                        artist = artist.asStateFlow(),
                        album = album.asStateFlow(),
                        searchResult = searchResult.asStateFlow(),
                        onNameChange = { name.value = it },
                        onAlbumChange = { album.value = it },
                        onArtistChange = { artist.value = it },
                        onSearchClick = {
                            coroutineScope.launch {
                                searching.value = true
                                try {
                                    val result = spotifySearchRepo.search(name.value, artist.value, album.value, null)
                                    searchResult.value = result.tracks?.items?.map { spotifyApiTrack ->
                                        Ui.State.Loaded.SpotifyTrackState.SearchResultItem(
                                            spotifyTrack = spotifyApiTrack,
                                            localId = spotifyTrackRepo.getId(spotifyApiTrack.id.value),
                                            setToTrack = trackRepo.getId(spotifyApiTrack.id.value)
                                        )
                                    } ?: emptyList()
                                    apiError.value = false
                                } catch (e: Throwable) {
                                    e.printStackTrace() // TODO()
                                    apiError.value = true
                                }
                                searching.value = false
                            }
                        },
                        onSearchItemClick = { item ->
                            coroutineScope.launch {
                                settingTrack.value = true
                                setTrackMetadataFromSpotifyUseCase.execute(
                                    trackId = id,
                                    spotifyApiTrack = item
                                )
                                settingTrack.value = false
                            }
                        },
                        onLinkedTrackClick = showTrack
                    ),
                    playlists = playlistRepo.getTrackPlaylists(id).mapLatest { dbList ->
                            dbList.map {
                                Ui.State.Loaded.Playlist(
                                    id = it.id,
                                    name = it.name,
                                    image = null
                                )
                            }
                        }.stateIn(coroutineScope),
                    onTabClick = { selectedTab.value = it },
                    onPlayClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(id))) },
                    onSetTag = { coroutineScope.launch { tagTrackCrossRefRepo.add(it, id) } },
                    onUnsetTag = { coroutineScope.launch { tagTrackCrossRefRepo.delete(it, id) } },
                    onTagDetailsClick = showTag,
                    onImportClick = showImportSessionItem,
                    onPlaylistClick = showPlaylist,
                    onRemoveFromPlaylistClick = {
                        coroutineScope.launch {
                            playlistTrackCrossRefRepo.delete(
                                it,
                                id
                            )
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

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }


    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val selectedTab: StateFlow<Tab>,
                val import: StateFlow<Import?>,
                val spotifyTrackState: SpotifyTrackState,
                val appliedTags: StateFlow<List<Tag>>,
                val unappliedTags: StateFlow<List<Tag>>,
                val playlists: StateFlow<List<Playlist>>,
                val onTabClick: (Tab) -> Unit,
                val onPlayClick: () -> Unit,
                val onSetTag: (TagId) -> Unit,
                val onUnsetTag: (TagId) -> Unit,
                val onTagDetailsClick: (TagId) -> Unit,
                val onImportClick: (ImportSessionItemId) -> Unit,
                val onPlaylistClick: (PlaylistId) -> Unit,
                val onRemoveFromPlaylistClick: (PlaylistId) -> Unit
            ) : State() {
                enum class Tab {
                    Import, SpotifyTrack, Tags, Playlists
                }

                data class Import(
                    val id: ImportSessionItemId,
                    val title: String,
                    val uri: String,
                    val duration: Duration?,
                    val artists: List<String>,
                    val album: String?,
                    val image: File?
                )

                data class SpotifyTrackState(
                    val apiError: StateFlow<Boolean>,
                    val spotifyTrack: StateFlow<SpotifyTrack?>,
                    val searching: StateFlow<Boolean>,
                    val settingTrack: StateFlow<Boolean>,
                    val name: StateFlow<String>,
                    val artist: StateFlow<String>,
                    val album: StateFlow<String>,
                    val searchResult: StateFlow<List<SearchResultItem>>,
                    val onNameChange: (String) -> Unit,
                    val onArtistChange: (String) -> Unit,
                    val onAlbumChange: (String) -> Unit,
                    val onSearchClick: () -> Unit,
                    val onSearchItemClick: (Track) -> Unit,
                    val onLinkedTrackClick: (TrackId) -> Unit
                ) {
                    data class SpotifyTrack(
                        val id: SpotifyTrackId,
                        val name: String,
                        val duration: Duration?,
                        val album: Album?,
                        val artists: List<Pair<SpotifyArtistId, String>>,
                        val image: File?,
                        val onAlbumClick: () -> Unit,
                        val onArtistClick: (SpotifyArtistId) -> Unit,
                        val onRemoveClick: () -> Unit
                    ) {
                        data class Album(
                            val id: SpotifyAlbumId,
                            val name: String
                        )
                    }

                    data class SearchResultItem(
                        val spotifyTrack: Track,
                        val localId: SpotifyTrackId?,
                        val setToTrack: TrackId?
                    )
                }

                data class Playlist(
                    val id: PlaylistId,
                    val name: String,
                    val image: File?
                )
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
                is State.Loaded -> Main(modifier = modifier, state = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            state: State.Loaded
        ) {
            Main(
                modifier = modifier,
                selectedTab = state.selectedTab,
                import = state.import,
                spotifyTrackState = state.spotifyTrackState,
                appliedTags = state.appliedTags,
                unappliedTags = state.unappliedTags,
                playlists = state.playlists,
                onTabClick = state.onTabClick,
                onPlayClick = state.onPlayClick,
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
            selectedTab: StateFlow<State.Loaded.Tab>,
            import: StateFlow<State.Loaded.Import?>,
            spotifyTrackState: State.Loaded.SpotifyTrackState,
            appliedTags: StateFlow<List<Tag>>,
            unappliedTags: StateFlow<List<Tag>>,
            playlists: StateFlow<List<State.Loaded.Playlist>>,
            onTabClick: (State.Loaded.Tab) -> Unit,
            onPlayClick: () -> Unit,
            onSetTag: (TagId) -> Unit,
            onUnsetTag: (TagId) -> Unit,
            onTagDetailsClick: (TagId) -> Unit,
            onImportClick: (ImportSessionItemId) -> Unit,
            onPlaylistClick: (PlaylistId) -> Unit,
            onRemoveFromPlaylistClick: (PlaylistId) -> Unit
        ) {
            val selectedTab by selectedTab.collectAsState()
            val import by import.collectAsState()
            val appliedTags by appliedTags.collectAsState()
            val unappliedTags by unappliedTags.collectAsState()
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
                    ExtendedFloatingActionButton(
                        onClick = onPlayClick,
                        icon = { Icon(Icons.Default.PlayCircle, null) },
                        text = { Text("Play") }
                    )
                    PrimaryScrollableTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = State.Loaded.Tab.entries.indexOf(selectedTab)
                    ) {
                        for (tab in State.Loaded.Tab.entries) {
                            Tab(
                                text = { Text(tab.name) },
                                selected = false,
                                onClick = { onTabClick(tab) }
                            )
                        }
                    }
                    when (selectedTab) {
                        State.Loaded.Tab.Import -> {
                            import?.let {
                                Import(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    import = it,
                                    onUriClick = { onImportClick(it.id) }
                                )
                            } ?: Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Import Session Item not found!")
                                }
                            }
                        }
                        State.Loaded.Tab.SpotifyTrack -> {
                            SpotifyTrack.Main(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                state = spotifyTrackState
                            )
                        }
                        State.Loaded.Tab.Tags -> Tags(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            applied = appliedTags,
                            unapplied = unappliedTags,
                            onSetTag = onSetTag,
                            onUnsetTag = onUnsetTag,
                            onTagDetailsClick = onTagDetailsClick
                        )
                        State.Loaded.Tab.Playlists -> Playlists(
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
        private fun Import(
            modifier: Modifier,
            import: State.Loaded.Import,
            onUriClick: () -> Unit
        ) {
            Surface(modifier) {
                AdaptiveUi(
                    wide = {
                        ItemDetailsHeaderWide(
                            title = import.title,
                            image = import.image,
                            itemAttributes = {
                                TextButton(onUriClick) {
                                    Text(
                                        text = import.uri,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Album(name = import.album ?: "")
                                Artists(names = import.artists)
                                Duration(value = import.duration.formatted())
                            }
                        )
                    },
                    compact = {
                        ItemDetailsHeaderCompact(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            title = import.title,
                            image = import.image,
                            itemAttributes = {
                                TextButton(onUriClick) {
                                    Text(
                                        text = import.uri,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Album(name = import.album ?: "")
                                Artists(names = import.artists)
                                Duration(value = import.duration.formatted())
                            }
                        )
                    }
                )
            }
        }

        private object SpotifyTrack {
            @Composable
            fun Main(modifier: Modifier, state: State.Loaded.SpotifyTrackState) {
                val apiError by state.apiError.collectAsState()
                val spotifyTrack by state.spotifyTrack.collectAsState()
                val searching by state.searching.collectAsState()
                val settingTrack by state.settingTrack.collectAsState()
                val name by state.name.collectAsState()
                val artist by state.artist.collectAsState()
                val album by state.album.collectAsState()
                val searchResult by state.searchResult.collectAsState()
                val onNameChange: (String) -> Unit = state.onNameChange
                val onArtistChange: (String) -> Unit = state.onArtistChange
                val onAlbumChange: (String) -> Unit = state.onAlbumChange
                val onSearchClick: () -> Unit = state.onSearchClick
                val onSearchItemClick: (Track) -> Unit = state.onSearchItemClick
                val onLinkedTrackClick: (TrackId) -> Unit = state.onLinkedTrackClick

                val inputEnabled = !searching && !settingTrack

                Surface(modifier.verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        spotifyTrack?.let {
                            SpotifyTrack(
                                modifier = Modifier.fillMaxWidth(),
                                track = it
                            )
                            HorizontalDivider()
                        }
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        OutlinedTextField(
                            label = { Text("Name") },
                            value = name,
                            onValueChange = onNameChange,
                            enabled = inputEnabled
                        )
                        OutlinedTextField(
                            label = { Text("Artist") },
                            value = artist,
                            onValueChange = onArtistChange,
                            enabled = inputEnabled
                        )
                        OutlinedTextField(
                            label = { Text("Album") },
                            value = album,
                            onValueChange = onAlbumChange,
                            enabled = inputEnabled
                        )
                        Button(
                            onClick = onSearchClick,
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (searching) {
                                        CircularProgressIndicator(Modifier.size(20.dp))
                                    }
                                    Text("Search")
                                }
                            },
                            enabled = inputEnabled
                        )
//                        if (!authorized) {
//                            Surface(
//                                shape = MaterialTheme.shapes.small,
//                                color = MaterialTheme.colorScheme.errorContainer
//                            ) {
//                                Text(
//                                    modifier = Modifier.padding(8.dp),
//                                    text = "Please check Spotify authorization in Settings",
//                                    style = MaterialTheme.typography.labelMedium
//                                )
//                            }
//                        }
                        if (apiError) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    modifier = Modifier.padding(8.dp),
                                    text = "Something went wrong while trying to get data from Spotify",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (settingTrack) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(Modifier.size(20.dp))
                                    Text(
                                        modifier = Modifier.padding(8.dp),
                                        text = "Please wait...",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                        if (inputEnabled) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(searchResult) { searchItem ->
                                    SearchResultItem(
                                        modifier = Modifier.width(200.dp),
                                        searchItem = searchItem,
                                        onClick = { onSearchItemClick(searchItem.spotifyTrack) },
                                        onLinkedTrackClick = {
                                            if (searchItem.setToTrack != null) {
                                                onLinkedTrackClick(searchItem.setToTrack)
                                            } else TODO()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun SpotifyTrack(
                modifier: Modifier,
                track: State.Loaded.SpotifyTrackState.SpotifyTrack
            ) {
                Surface(modifier) {
                    AdaptiveUi(
                        wide = {
                            ItemDetailsHeaderWide(
                                modifier = Modifier.height(500.dp),
                                title = track.name,
                                image = track.image,
                                itemAttributes = {
                                    if (track.album != null) {
                                        Album(name = track.album.name, onClick = track.onAlbumClick)
                                    } else { // TODO
                                        Album(name = "")
                                    }
                                    Artists(artists = track.artists, onArtistClick = track.onArtistClick)
                                    Duration(value = track.duration.formatted())
                                },
                                actions = listOf(
                                    HeaderAction(
                                        "Remove",
                                        Icons.Default.Remove,
                                        track.onRemoveClick
                                    )
                                )
                            )
                        },
                        compact = {
                            ItemDetailsHeaderCompact(
                                title = track.name,
                                image = track.image,
                                itemAttributes = {
                                    if (track.album != null) {
                                        Album(name = track.album.name, onClick = track.onAlbumClick)
                                    } else { // TODO
                                        Album(name = "")
                                    }
                                    Artists(artists = track.artists, onArtistClick = track.onArtistClick)
                                    Duration(value = track.duration.formatted())
                                },
                                actions = listOf(
                                    HeaderAction(
                                        "Remove",
                                        Icons.Default.Remove,
                                        track.onRemoveClick
                                    )
                                )
                            )
                        }
                    )
                }
            }

            @Composable
            private fun SearchResultItem(
                modifier: Modifier = Modifier,
                searchItem: State.Loaded.SpotifyTrackState.SearchResultItem,
                onClick: () -> Unit,
                onLinkedTrackClick: () -> Unit
            ) {
                val (spotifyTrack, localId, setToTrack) = searchItem
                Item(
                    modifier = modifier,
                    contentPadding = PaddingValues(8.dp),
                    onClick = if (setToTrack == null) onClick else null
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            url = spotifyTrack.album.images?.let {
                                if (it.getOrNull(1) != null) {
                                    it[1].url
                                } else if (it.getOrNull(2) != null) {
                                    it[2].url
                                } else if (it.getOrNull(0) != null) {
                                    it[0].url
                                } else null
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = spotifyTrack.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        if (localId != null) {
                            Icon(Icons.Default.DownloadDone, null)
                            Spacer(Modifier.height(8.dp))
                        }
                        Album(
                            name = spotifyTrack.album.name
                        )
                        Spacer(Modifier.height(8.dp))
                        Artists(
                            names = spotifyTrack.artists.map { it.name })
                        Spacer(Modifier.height(8.dp))
                        Duration(
                            value = spotifyTrack.durationMs?.milliseconds?.formatted() ?: "??:??"
                        )
                        Spacer(Modifier.height(8.dp))
                        if (setToTrack != null) {
                            TextButton(onLinkedTrackClick) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, null)
                                    Text(
                                        text = "A local track is linked with this spotify track",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun Playlists(
            modifier: Modifier,
            playlists: List<State.Loaded.Playlist>,
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
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        onClick = { onPlaylistClick(playlist.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                                file = playlist.image
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium
                            )
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