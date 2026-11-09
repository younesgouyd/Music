package dev.younesgouyd.apps.music.client.multiplatform.components

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
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrackList(
    trackRepo: TrackRepo,
    tagRepo: TagRepo,
    mediaController: MediaController,
    showTrack: (TrackId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Tracks"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        val searchQuery = MutableStateFlow("")
        val tagSearchQuery = MutableStateFlow("")
        val selectedTags = MutableStateFlow(emptyList<TagId>())
        val enableFiltering = MutableStateFlow(true)
        val includeUntagged = MutableStateFlow(true)

        coroutineScope.launch {
            val tracks = LazilyLoadedItems(
                scope = coroutineScope,
                loader = combine(
                    searchQuery, selectedTags,
                    enableFiltering, includeUntagged
                ) { searchQuery, selectedTags, enableFiltering, includeUntagged ->
                    Filter(
                        searchQuery = searchQuery,
                        selectedTags = selectedTags,
                        enableFiltering = enableFiltering,
                        includeUntagged = includeUntagged
                    )
                }.mapLatest {
                    Loader(
                        filter = it,
                        trackRepo = trackRepo
                    )
                }.stateIn(coroutineScope),
                initialOffset = Offset.Id.initial<TrackId>()
            )
            state.value = Ui.State.Loaded(
                scrollState = LazyGridState(),
                tracks = tracks,
                tagsFilterState = TagsFilterState(
                    tags = combine(tagSearchQuery, selectedTags, ::Pair)
                        .flatMapLatest { (search, selected) ->
                            tagRepo.search(search).map { dbList ->
                                dbList.sortedByDescending { selected.contains(it.id) }
                                    .map { dbTag ->
                                        TagsFilterState.Tag(
                                            id = dbTag.id,
                                            name = dbTag.name,
                                            selected = selected.contains(dbTag.id)
                                        )
                                    }
                            }
                        }.stateIn(coroutineScope),
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
                            tracks.items.value.map { QueueItemParameter.Track(it.id) }
                        )
                    }
                },
                onSearchQueryChange = { searchQuery.value = it },
                onTrackClick = { mediaController.playQueue(listOf(QueueItemParameter.Track(it))) },
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

    private data class Filter(
        val searchQuery: String,
        val selectedTags: List<TagId>,
        val enableFiltering: Boolean,
        val includeUntagged: Boolean
    )

    private class Loader(
        private val filter: Filter,
        private val trackRepo: TrackRepo,
    ) : LazilyLoadedItems.Loader<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
        override suspend fun load(
            offset: Offset.Id<TrackId>,
            pageSize: PageSize
        ): LazilyLoadedItems.Page<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
            return if (filter.enableFiltering) {
                trackRepo.searchWithTags(
                    nameQuery = filter.searchQuery,
                    tags = filter.selectedTags,
                    includeUntagged = filter.includeUntagged,
                    limit = pageSize,
                    offset = offset
                ).toModel()
            } else {
                trackRepo.search(
                    nameQuery = filter.searchQuery,
                    limit = pageSize,
                    offset = offset
                ).toModel()
            }
        }

        private fun List<TrackRelation>.toModel(): LazilyLoadedItems.Page<Ui.State.Loaded.Track, Offset.Id<TrackId>> {
            return LazilyLoadedItems.Page(
                nextOffset = this.lastOrNull()?.track?.id?.let { Offset.Id(it) },
                items = this.map { dbTrack ->
                    Ui.State.Loaded.Track(
                        id = dbTrack.track.id,
                        name = dbTrack.spotifyTrackRelation?.spotifyTrack?.name ?: dbTrack.originalImport.title,
                        image = dbTrack.spotifyTrackRelation?.spotifyAlbum?.mediumImgId ?: dbTrack.originalImport.imgId,
                        artists = run {
                            dbTrack.spotifyTrackRelation?.spotifyArtists?.map {
                                Ui.State.Loaded.Track.Artist(it.id, it.name)
                            } ?: dbTrack.originalImport.inspection.artists.map {
                                Ui.State.Loaded.Track.Artist(null, it)
                            }
                        }
                    )
                }
            )
        }
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            class Loaded(
                val scrollState: LazyGridState,
                val tracks: LazilyLoadedItems<Track, Offset.Id<TrackId>>,
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
                    val image: MediaFileId?,
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
            tracks: LazilyLoadedItems<State.Loaded.Track, Offset.Id<TrackId>>,
            tagsFilterState: TagsFilterState,
            searchQuery: StateFlow<String>,
            onPlayClick: () -> Unit,
            onSearchQueryChange: (String) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onTrackDetailsClick: (TrackId) -> Unit
        ) {
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
                    .distinctUntilChanged()
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
                        id = track.image,
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
