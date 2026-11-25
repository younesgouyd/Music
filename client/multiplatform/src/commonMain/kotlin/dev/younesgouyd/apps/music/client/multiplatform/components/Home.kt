package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.components.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.client.multiplatform.components.util.SearchBar
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.HomeRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagRepo
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.Tag
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import dev.younesgouyd.apps.music.common.models.datamodels.home.HomeItems
import dev.younesgouyd.apps.music.common.models.pagination.Offset
import dev.younesgouyd.apps.music.common.ui.Component
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class Home(
    private val homeRepo: HomeRepo,
    private val tagRepo: TagRepo
) : Component() {
    override val title: String = "Home"

    private val _query = MutableStateFlow("")
    private val query = _query.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<HomeItemType>>(emptySet())
    private val selectedTypes = _selectedTypes.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<TagId>>(emptySet())
    private val selectedTags = _selectedTags.asStateFlow()

    private val availableTags: StateFlow<List<Tag>> = tagRepo.search("")
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(FlowPreview::class)
    private val loader: StateFlow<LazilyLoadedItems.Loader<HomeItems, Offset.Home>> =
        combine(
            _query.debounce(250.milliseconds),
            _selectedTypes,
            _selectedTags
        ) { q, types, tags ->
            LazilyLoadedItems.Loader<HomeItems, Offset.Home> { offset, _ ->
                homeRepo.getPage(
                    query = q.trim(),
                    selectedTypes = types,
                    selectedTags = tags,
                    offset = offset
                )
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = LazilyLoadedItems.Loader { offset, _ ->
                homeRepo.getPage(
                    query = "",
                    selectedTypes = emptySet(),
                    selectedTags = emptySet(),
                    offset = offset
                )
            }
        )

    private val lazilyLoadedItems = LazilyLoadedItems(
        scope = coroutineScope,
        loader = loader,
        initialOffset = Offset.Home.initial()
    )

    // Flatten paginated HomeItems chunks into a single UI feed list
    private val feedEntries: StateFlow<List<HomeFeedEntry>> = lazilyLoadedItems.items
        .map { pages -> pages.flatMap { it.toFeedEntries() } }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @Composable
    override fun show(modifier: Modifier) {
        val currentQuery by query.collectAsState()
        val currentTypes by selectedTypes.collectAsState()
        val currentTags by selectedTags.collectAsState()
        val tags by availableTags.collectAsState()
        val entries by feedEntries.collectAsState()
        val isLoading by lazilyLoadedItems.loading.collectAsState()

        Ui.Main(
            modifier = modifier,
            query = currentQuery,
            onQueryChange = { _query.value = it },
            selectedTypes = currentTypes,
            onToggleType = ::toggleType,
            availableTags = tags,
            selectedTags = currentTags,
            onToggleTag = ::toggleTag,
            entries = entries,
            isLoading = isLoading,
            onLoadMore = { lazilyLoadedItems.loadMore() }
        )
    }

    private fun toggleType(type: HomeItemType) {
        _selectedTypes.update { current ->
            if (type in current) current - type else current + type
        }
    }

    private fun toggleTag(tagId: TagId) {
        _selectedTags.update { current ->
            val updated = if (tagId in current) current - tagId else current + tagId
            // Selecting a tag restricts results to Tracks only; clear incompatible type filters
            if (updated.isNotEmpty()) {
                _selectedTypes.update { types ->
                    if (HomeItemType.TRACK in types) setOf(HomeItemType.TRACK) else emptySet()
                }
            }
            updated
        }
    }

    private fun List<TrackRelation>.toTrackModels(): List<Track> {
        return this.map { dbTrack ->
            Track(
                id = dbTrack.track.id,
                importSessionItemId = dbTrack.track.importSessionItemId,
                name = dbTrack.spotifyTrackRelation?.spotifyTrack?.name ?: dbTrack.originalImport.title,
                image = dbTrack.spotifyTrackRelation?.spotifyAlbum?.mediumImgId ?: dbTrack.originalImport.imgId,
                artists = run {
                    dbTrack.spotifyTrackRelation?.spotifyArtists?.map {
                        Track.Artist(it.id, it.name)
                    } ?: dbTrack.originalImport.inspection.artists.map {
                        Track.Artist(null, it)
                    }
                }
            )
        }
    }

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

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed interface HomeFeedEntry {
        val key: String
        val label: String
        val typeLabel: String

        data class TrackEntry(override val key: String, override val label: String) : HomeFeedEntry {
            override val typeLabel: String = "Track"
        }
        data class PlaylistEntry(override val key: String, override val label: String) : HomeFeedEntry {
            override val typeLabel: String = "Playlist"
        }
        data class AlbumEntry(override val key: String, override val label: String) : HomeFeedEntry {
            override val typeLabel: String = "Album"
        }
        data class ArtistEntry(override val key: String, override val label: String) : HomeFeedEntry {
            override val typeLabel: String = "Artist"
        }
        data class TagEntry(override val key: String, override val label: String) : HomeFeedEntry {
            override val typeLabel: String = "Tag"
        }
    }

    private fun HomeItems.toFeedEntries(): List<HomeFeedEntry> = buildList {
        tracks.forEach { add(HomeFeedEntry.TrackEntry(it.track.id.key, it.originalImport.title)) }
        playlists.forEach { add(HomeFeedEntry.PlaylistEntry(it.id.key, it.name)) }
        albums.forEach { add(HomeFeedEntry.AlbumEntry(it.id.key, it.name)) }
        artists.forEach { add(HomeFeedEntry.ArtistEntry(it.id.key, it.name)) }
        tags.forEach { add(HomeFeedEntry.TagEntry(it.id.key, it.name)) }
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            query: String,
            onQueryChange: (String) -> Unit,
            selectedTypes: Set<HomeItemType>,
            onToggleType: (HomeItemType) -> Unit,
            availableTags: List<Tag>,
            selectedTags: Set<TagId>,
            onToggleTag: (TagId) -> Unit,
            entries: List<HomeFeedEntry>,
            isLoading: Boolean,
            onLoadMore: suspend () -> Unit
        ) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBar(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = "Search tracks, albums, artists..."
                    )

                    TypeFilterRow(
                        selectedTypes = selectedTypes,
                        tagsSelected = selectedTags.isNotEmpty(),
                        onToggleType = onToggleType
                    )

                    if (availableTags.isNotEmpty()) {
                        TagFilterRow(
                            availableTags = availableTags,
                            selectedTags = selectedTags,
                            onToggleTag = onToggleTag
                        )
                    }

                    FeedList(
                        modifier = Modifier.weight(1f),
                        entries = entries,
                        isLoading = isLoading,
                        onLoadMore = onLoadMore
                    )
                }
            }
        }

        @Composable
        private fun TypeFilterRow(
            selectedTypes: Set<HomeItemType>,
            tagsSelected: Boolean,
            onToggleType: (HomeItemType) -> Unit
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(HomeItemType.entries) { type ->
                    val selected = type in selectedTypes
                    FilterChip(
                        selected = selected,
                        enabled = !tagsSelected || type == HomeItemType.TRACK,
                        onClick = { onToggleType(type) },
                        label = { Text(type.name) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        @Composable
        private fun TagFilterRow(
            availableTags: List<Tag>,
            selectedTags: Set<TagId>,
            onToggleTag: (TagId) -> Unit
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(availableTags, key = { it.id.value }) { tag ->
                    val selected = tag.id in selectedTags
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleTag(tag.id) },
                        label = { Text(tag.name) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        @Composable
        private fun FeedList(
            modifier: Modifier = Modifier,
            entries: List<HomeFeedEntry>,
            isLoading: Boolean,
            onLoadMore: suspend () -> Unit
        ) {
            val listState = rememberLazyListState()

            val shouldLoadMore by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    layoutInfo.totalItemsCount > 0 && lastVisibleIndex >= layoutInfo.totalItemsCount - 5
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) {
                    onLoadMore()
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(entries, key = { it.key }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.label) },
                        supportingContent = { Text(entry.typeLabel) }
                    )
                }

                if (isLoading) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}