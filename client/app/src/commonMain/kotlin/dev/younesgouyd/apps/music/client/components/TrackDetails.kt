package dev.younesgouyd.apps.music.client.components

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
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.usecases.SetTrackMetadataFromSpotifyUseCase
import dev.younesgouyd.apps.music.client.usecases.UnsetSpotifyTrackUseCase
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.libs.music.spotifyapi.InvalidCredentials
import dev.younesgouyd.libs.music.spotifyapi.SpotifyApi
import dev.younesgouyd.libs.music.spotifyapi.models.Track
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
    artistRepo: SpotifyArtistRepo,
    playlistRepo: PlaylistRepo,
    tagTrackCrossRefRepo: TagTrackCrossRefRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    mediaFileRepo: MediaFileRepo,
    setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
    unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    mediaController: MediaController,
    spotifyApi: SpotifyApi,
    showTag: (TagId) -> Unit,
    showPlaylist: (PlaylistId) -> Unit,
    showImportSessionItem: (ImportSessionItemId) -> Unit,
    showTrack: (TrackId) -> Unit,
    showAlbum: (SpotifyAlbumId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Track"
    private val selectedTab = MutableStateFlow(TrackDetailsState.Loaded.Tab.Import)
    private val spotifyTrackState: MutableStateFlow<TrackDetailsState.Loaded.SpotifyTrackState> = MutableStateFlow(TrackDetailsState.Loaded.SpotifyTrackState.Loading)
    private val state: MutableStateFlow<TrackDetailsState> = MutableStateFlow(TrackDetailsState.Loading)

    init {
        coroutineScope.launch {
            val track = trackRepo.get(id).stateIn(coroutineScope)
            state.value = TrackDetailsState.Loaded(
                selectedTab = selectedTab.asStateFlow(),
                import = track.map { dbImport ->
                    TrackDetailsState.Loaded.Import(
                        id = dbImport.originalImport.id,
                        title = dbImport.originalImport.title,
                        uri = dbImport.originalImport.uri,
                        duration = dbImport.originalImport.durationMilliseconds.milliseconds,
                        artists = dbImport.originalImport.inspection.artists,
                        album = dbImport.originalImport.album,
                        image = mediaFileRepo.getImportSessionItemImage(dbImport.originalImport.id)
                    )
                }.stateIn(coroutineScope),
                appliedTags = tagRepo.getTrackTags(id).map {
                    it.map { TrackDetailsState.Loaded.Tag(it.id, it.name) }
                }.stateIn(coroutineScope),
                unappliedTags = tagRepo.getTrackUnsetTags(id).map {
                    it.map { TrackDetailsState.Loaded.Tag(it.id, it.name) }
                }.stateIn(coroutineScope),
                spotifyTrackState = spotifyTrackState.asStateFlow(),
                playlists = playlistRepo.getTrackPlaylists(id)
                    .map { dbList ->
                        dbList.map {
                            TrackDetailsState.Loaded.Playlist(
                            id = it.id,
                            name = it.name,
                            image = it.importSessionId?.let { mediaFileRepo.getImportSessionImage(it) }
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
                onRemoveFromPlaylistClick = { coroutineScope.launch { playlistTrackCrossRefRepo.delete(it, id) } }
            )
            launch {
                val authorized = MutableStateFlow(true) // TODO
                val apiError = MutableStateFlow(false)
                val spotifyTrack = MutableStateFlow<TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SpotifyTrack?>(null)
                val searching = MutableStateFlow(false)
                val settingTrack = MutableStateFlow(false)
                val name = MutableStateFlow("")
                val artist = MutableStateFlow("")
                val album = MutableStateFlow("")
                val searchResult = MutableStateFlow(emptyList<TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SearchResultItem>())
                spotifyTrackState.value = TrackDetailsState.Loaded.SpotifyTrackState.Loaded(
                    authorized = authorized.asStateFlow(),
                    apiError = apiError.asStateFlow(),
                    spotifyTrack = spotifyTrack.asStateFlow(),
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
                                val result = spotifyApi.search(name.value, artist.value, album.value, null)
                                searchResult.value = result.tracks?.items?.map { spotifyApiTrack ->
                                    TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SearchResultItem(
                                        spotifyTrack = spotifyApiTrack,
                                        localId = spotifyTrackRepo.getId(spotifyApiTrack.id.value),
                                        setToTrack = trackRepo.getId(spotifyApiTrack.id.value)
                                    )
                                } ?: emptyList()
                                apiError.value = false
                                authorized.value = true
                            } catch (_: InvalidCredentials) {
                                authorized.value = false
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
                )
                track.collect { dbTrack ->
                    spotifyTrack.value = dbTrack.spotifyTrack?.let { spotifyTrack ->
                        TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SpotifyTrack(
                            id = spotifyTrack.id,
                            name = spotifyTrack.name,
                            duration = spotifyTrack.durationMs?.milliseconds,
                            album = albumRepo.get(spotifyTrack.spotifyAlbumId).first().let {
                                TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SpotifyTrack.Album(it.id, it.name)
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
                                        spotifyTrackId = dbTrack.spotifyTrack.id,
                                        spotifyAlbumId = dbTrack.spotifyTrack.spotifyAlbumId
                                    )
                                }
                            }
                        )
                    }
                    name.value = dbTrack.originalImport.title
                    artist.value = dbTrack.originalImport.inspection.artists.firstOrNull() ?: ""
                    album.value = dbTrack.originalImport.album ?: ""
                }
            }
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
            val import: StateFlow<Import>,
            val spotifyTrackState: StateFlow<SpotifyTrackState>,
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
        ) : TrackDetailsState() {
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

            sealed class SpotifyTrackState {
                data object Loading : SpotifyTrackState()

                data class Loaded(
                    val authorized: StateFlow<Boolean>,
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
                ) : SpotifyTrackState() {
                    data class SpotifyTrack(
                        val id: SpotifyTrackId,
                        val name: String,
                        val duration: Duration?,
                        val album: Album,
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
            }

            data class Tag(
                val id: TagId,
                val name: String
            )

            data class Playlist(
                val id: PlaylistId,
                val name: String,
                val image: File?
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
            selectedTab: StateFlow<TrackDetailsState.Loaded.Tab>,
            import: StateFlow<TrackDetailsState.Loaded.Import>,
            spotifyTrackState: StateFlow<TrackDetailsState.Loaded.SpotifyTrackState>,
            appliedTags: StateFlow<List<TrackDetailsState.Loaded.Tag>>,
            unappliedTags: StateFlow<List<TrackDetailsState.Loaded.Tag>>,
            playlists: StateFlow<List<TrackDetailsState.Loaded.Playlist>>,
            onTabClick: (TrackDetailsState.Loaded.Tab) -> Unit,
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
            val spotifyTrackState by spotifyTrackState.collectAsState()
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
                        TrackDetailsState.Loaded.Tab.Import -> {
                            Import(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                import = import,
                                onUriClick = { onImportClick(import.id) }
                            )
                        }
                        TrackDetailsState.Loaded.Tab.SpotifyTrack -> {
                            SpotifyTrack.Main(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                state = spotifyTrackState
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
        private fun Import(
            modifier: Modifier,
            import: TrackDetailsState.Loaded.Import,
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
            fun Main(modifier: Modifier, state: TrackDetailsState.Loaded.SpotifyTrackState) {
                when (state) {
                    is TrackDetailsState.Loaded.SpotifyTrackState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is TrackDetailsState.Loaded.SpotifyTrackState.Loaded -> Main(modifier, state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, state: TrackDetailsState.Loaded.SpotifyTrackState.Loaded) {
                val authorized by state.authorized.collectAsState()
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

                val inputEnabled = !searching && !settingTrack && authorized

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
                        if (!authorized) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    modifier = Modifier.padding(8.dp),
                                    text = "Please check Spotify authorization in Settings",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
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
                track: TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SpotifyTrack
            ) {
                Surface(modifier) {
                    AdaptiveUi(
                        wide = {
                            ItemDetailsHeaderWide(
                                modifier = Modifier.height(500.dp),
                                title = track.name,
                                image = track.image,
                                itemAttributes = {
                                    Album(
                                        name = track.album.name,
                                        onClick = track.onAlbumClick
                                    )
                                    Artists(
                                        artists = track.artists,
                                        onArtistClick = track.onArtistClick
                                    )
                                    Duration(value = track.duration.formatted())
                                },
                                actions = listOf(HeaderAction("Remove", Icons.Default.Remove, track.onRemoveClick))
                            )
                        },
                        compact = {
                            ItemDetailsHeaderCompact(
                                title = track.name,
                                image = track.image,
                                itemAttributes = {
                                    Album(
                                        name = track.album.name,
                                        onClick = track.onAlbumClick
                                    )
                                    Artists(
                                        artists = track.artists,
                                        onArtistClick = track.onArtistClick
                                    )
                                    Duration(value = track.duration.formatted())
                                },
                                actions = listOf(HeaderAction("Remove", Icons.Default.Remove, track.onRemoveClick))
                            )
                        }
                    )
                }
            }

            @Composable
            private fun SearchResultItem(
                modifier: Modifier = Modifier,
                searchItem: TrackDetailsState.Loaded.SpotifyTrackState.Loaded.SearchResultItem,
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
                        Album(name = spotifyTrack.album.name)
                        Spacer(Modifier.height(8.dp))
                        Artists(names = spotifyTrack.artists.map { it.name })
                        Spacer(Modifier.height(8.dp))
                        Duration(value = spotifyTrack.durationMs?.milliseconds?.formatted() ?: "??:??")
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
            playlists: List<TrackDetailsState.Loaded.Playlist>,
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
            applied: List<TrackDetailsState.Loaded.Tag>,
            unapplied: List<TrackDetailsState.Loaded.Tag>,
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