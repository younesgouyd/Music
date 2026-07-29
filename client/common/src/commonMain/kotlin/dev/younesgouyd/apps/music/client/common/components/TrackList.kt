package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.MediaController
import dev.younesgouyd.apps.music.client.common.components.util.*
import dev.younesgouyd.apps.music.client.common.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.client.common.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.client.common.util.PageSize
import dev.younesgouyd.apps.music.common.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TrackList(
    trackRepo: TrackRepo,
    tagRepo: TagRepo,
    artistRepo: SpotifyArtistRepo,
    mediaFileRepo: MediaFileRepo,
    mediaController: MediaController,
    showTrack: (TrackId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Tracks"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    private data class Filter(
        val search: String,
        val tags: List<TagId>,
        val enableFiltering: Boolean,
        val includeUntagged: Boolean
    )
    init {
        val searchQuery = MutableStateFlow("")
        val tagSearchQuery = MutableStateFlow("")
        val selectedTags = MutableStateFlow(emptyList<TagId>())
        val enableFiltering = MutableStateFlow(true)
        val includeUntagged = MutableStateFlow(true)

        suspend fun List<TrackRelation>.toModel(): LazilyLoadedItems.Page<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
            return LazilyLoadedItems.Page(
                nextOffset = this.lastOrNull()?.track?.id?.let { Offset.Id(it) },
                items = this.map { dbTrack ->
                    Ui.State.Loaded.Track(
                        id = dbTrack.track.id,
                        name = dbTrack.spotifyTrack?.name ?: dbTrack.originalImport.title,
                        image = if (dbTrack.spotifyTrack != null) {
                            mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack!!.spotifyAlbumId)
                        } else {
                            mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
                        },
                        artists = if (dbTrack.spotifyTrack != null) {
                            artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack!!.id).first()
                                .map { dbArtist ->
                                    Ui.State.Loaded.Track.Artist(dbArtist.id, dbArtist.name)
                                }
                        } else {
                            dbTrack.originalImport.inspection.artists.map {
                                Ui.State.Loaded.Track.Artist(null, it)
                            }
                        }
                    )
                }
            )
        }

        val tracks = combine(
            searchQuery, selectedTags,
            enableFiltering, includeUntagged
        ) { search, tags, enableFiltering, untagged -> Filter(search, tags, enableFiltering, untagged) }
            .mapLatest { (search, tags, enableFiltering, untagged) ->
                when (enableFiltering) {
                    true -> {
                        LazilyLoadedItems(
                            coroutineScope = coroutineScope,
                            load = { offset, pageSize: PageSize ->
                                trackRepo.search(
                                    nameQuery = search,
                                    tags = tags,
                                    includeUntagged = untagged,
                                    limit = pageSize,
                                    offset = offset
                                ).toModel()
                            },
                            initialOffset = Offset.Id.initial<TrackId>()
                        )
                    }
                    false -> {
                        LazilyLoadedItems(
                            coroutineScope = coroutineScope,
                            load = { offset, pageSize: PageSize ->
                                trackRepo.search(
                                    nameQuery = search,
                                    limit = pageSize,
                                    offset = offset
                                ).toModel()
                            },
                            initialOffset = Offset.Id.initial<TrackId>()
                        )
                    }
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                scrollState = LazyGridState(),
                tracks = tracks.filterNotNull().stateIn(coroutineScope),
                tagsFilterState = TagsFilterState(
                    tags = combine(
                        tagSearchQuery, selectedTags
                    ) { search, selected -> Pair(search, selected) }
                        .flatMapLatest { (search, selected) ->
                            tagRepo.search(search).map {
                                it.sortedWith { first, second ->
                                    val b1 = selected.contains(first.id)
                                    val b2 = selected.contains(second.id)
                                    if (b1 && b2) 0
                                    else if (b1) -1
                                    else 1
                                }.map { dbTag ->
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
                searchQuery = searchQuery.asStateFlow(),
                onPlayClick = {
                    coroutineScope.launch {
                        mediaController.playQueue(
                            tracks.value?.items?.value.orEmpty().map { MediaController.QueueItemParameter.Track(it.id) }
                        )
                    }
                },
                onSearchQueryChange = { searchQuery.value = it },
                onTrackClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(it))) },
                onArtistClick = showArtist,
                onTrackDetailsClick = showTrack
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val scrollState: LazyGridState,
                val tracks: StateFlow<LazilyLoadedItems<Track, Offset.Id<TrackId>>>,
                val tagsFilterState: TagsFilterState,
                val searchQuery: StateFlow<String>,
                val onPlayClick: () -> Unit,
                val onSearchQueryChange: (String) -> Unit,
                val onTrackClick: (TrackId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onTrackDetailsClick: (TrackId) -> Unit
            ) : State() {
                data class Track(
                    val id: TrackId,
                    val name: String,
                    val image: File?,
                    val artists: List<Artist>
                ) {
                    data class Artist(
                        val id: SpotifyArtistId?,
                        val name: String
                    )
                }
            }
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier, state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: State.Loaded) {
            Main(
                modifier = modifier,
                scrollState = state.scrollState,
                tracks = state.tracks,
                tagsFilterState = state.tagsFilterState,
                searchQuery = state.searchQuery,
                onPlayClick = state.onPlayClick,
                onSearchQueryChange = state.onSearchQueryChange,
                onTrackClick = state.onTrackClick,
                onArtistClick = state.onArtistClick,
                onTrackDetailsClick = state.onTrackDetailsClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            scrollState: LazyGridState,
            tracks: StateFlow<LazilyLoadedItems<State.Loaded.Track, Offset.Id<TrackId>>>,
            tagsFilterState: TagsFilterState,
            searchQuery: StateFlow<String>,
            onPlayClick: () -> Unit,
            onSearchQueryChange: (String) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onTrackDetailsClick: (TrackId) -> Unit
        ) {
            val tracks by tracks.collectAsState()
            val items by tracks.items.collectAsState()
            val loadingItems by tracks.loading.collectAsState()
            val searchQuery by searchQuery.collectAsState()

            Scaffold(
                modifier = modifier,
                content = { paddingValues ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.size(12.dp))
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search") },
                            value = searchQuery,
                            onValueChange = onSearchQueryChange
                        )
                        TagsFilter(
                            modifier = Modifier.fillMaxWidth(),
                            state = tagsFilterState
                        )
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            state = scrollState,
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            columns = GridCells.Adaptive(
                                minSize = when (getWindowSizeClass()) {
                                    WindowWidthSizeClass.Compact -> { 100.dp }
                                    else -> { 200.dp }
                                }
                            )
                        ) {
                            items(items) { track ->
                                TrackItem(
                                    track = track,
                                    onClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onDetailsClick = { onTrackDetailsClick(track.id) }
                                )
                            }
                            if (loadingItems) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (items.isNotEmpty()) {
                        LargeFloatingActionButton(
                            content = { Icon(Icons.Default.PlayCircle, null) },
                            onClick = onPlayClick
                        )
                    }
                }
            )

            LaunchedEffect(scrollState, tracks) {
                snapshotFlow {
                    scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                }.map { it == null ||  it >= items.size - 5  }
                    .filter { it }
                    .collect { tracks.loadMore() }
            }
        }

        @Composable
        private fun TrackItem(
            track: State.Loaded.Track,
            onClick: () -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onDetailsClick: () -> Unit
        ) {
            Item(onClick = onClick) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        file = track.image,
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
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = { onArtistClick(artist.id!!) },
                                    enabled = artist.id != null
                                )
                            }
                        }
                        IconButton(
                            content = { Icon(Icons.Default.Info, null) },
                            onClick = onDetailsClick
                        )
                    }
                }
            }
        }
    }
}
