package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.components.util.Image
import dev.younesgouyd.apps.music.client.components.util.Item
import dev.younesgouyd.apps.music.client.components.util.TagsFilter
import dev.younesgouyd.apps.music.client.components.util.TagsFilterState
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.data.room.entities.Track
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TrackList(
    trackRepo: TrackRepo,
    tagRepo: TagRepo,
    artistRepo: ArtistRepo,
    mediaFileRepo: MediaFileRepo,
    mediaController: MediaController,
    showTrack: (TrackId) -> Unit,
    showArtist: (ArtistId) -> Unit
) : Component() {
    override val title: String = "Tracks"
    private val state: State

    init {
        val searchQuery = MutableStateFlow("")
        val tagSearchQuery = MutableStateFlow("")
        val selectedTags = MutableStateFlow(emptyList<TagId>())
        val includeUntagged = MutableStateFlow(false)
        val tracks = combine(searchQuery, selectedTags, includeUntagged) { search, tags, untagged -> Triple(search, tags, untagged) }
            .flatMapLatest { (search, tags, untagged) ->  trackRepo.search(search, tags, untagged) }
            .map { dbTracks -> dbTracks.toModel(mediaFileRepo, artistRepo) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
        state = State(
            scrollState = LazyGridState(),
            tracks = tracks,
            tagsFilterState = TagsFilterState(
                tags = combine(tagSearchQuery, selectedTags, includeUntagged) { search, selected, untagged -> Triple(search, selected, untagged) }
                    .flatMapLatest { (search, selected, untagged) ->
                        tagRepo.search(search).map {
                            it.map { dbTag ->
                                TagsFilterState.Tag(
                                    id = dbTag.id,
                                    name = dbTag.name,
                                    selected = selected.contains(dbTag.id)
                                )
                            }
                        }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                searchQuery = tagSearchQuery,
                includeUntagged = includeUntagged.asStateFlow(),
                onSearchQueryChange = { tagSearchQuery.value = it },
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
                        tracks.value.map { MediaController.QueueItemParameter.Track(it.id) }
                    )
                }
            },
            onSearchQueryChange = { searchQuery.value = it },
            onTrackClick = showTrack,
            onArtistClick = showArtist,
            onPlayTrackClick = { mediaController.playQueue(listOf(MediaController.QueueItemParameter.Track(it))) }
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun List<Track>.toModel(mediaFileRepo: MediaFileRepo, artistRepo: ArtistRepo): List<State.Track> {
        return this.map { dbTrack ->
            State.Track(
                id = dbTrack.id,
                name = dbTrack.name,
                image = mediaFileRepo.getTrackImage(dbTrack.id),
                artists = artistRepo.getTrackArtists(dbTrack.id).first().map {dbArtist ->
                    State.Track.Artist(
                        id = dbArtist.id,
                        name = dbArtist.name
                    )
                }
            )
        }
    }

    data class State(
        val scrollState: LazyGridState,
        val tracks: StateFlow<List<Track>>,
        val tagsFilterState: TagsFilterState,
        val searchQuery: StateFlow<String>,
        val onPlayClick: () -> Unit,
        val onSearchQueryChange: (String) -> Unit,
        val onTrackClick: (TrackId) -> Unit,
        val onArtistClick: (ArtistId) -> Unit,
        val onPlayTrackClick: (TrackId) -> Unit
    ) {
        data class Track(
            val id: TrackId,
            val name: String,
            val image: File?,
            val artists: List<Artist>
        ) {
            data class Artist(
                val id: ArtistId,
                val name: String
            )
        }
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: State) {
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
                onPlayTrackClick = state.onPlayTrackClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            scrollState: LazyGridState,
            tracks: StateFlow<List<State.Track>>,
            tagsFilterState: TagsFilterState,
            searchQuery: StateFlow<String>,
            onPlayClick: () -> Unit,
            onSearchQueryChange: (String) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onPlayTrackClick: (TrackId) -> Unit
        ) {
            val tracks by tracks.collectAsState()
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
                            columns = GridCells.Adaptive(200.dp)
                        ) {
                            items(tracks, { it.id.value }) { track ->
                                TrackItem(
                                    track = track,
                                    onClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onPlayClick = { onPlayTrackClick(track.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (tracks.isNotEmpty()) {
                        LargeFloatingActionButton(
                            content = { Icon(Icons.Default.PlayCircle, null) },
                            onClick = onPlayClick
                        )
                    }
                }
            )
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            track: State.Track,
            onClick: () -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onPlayClick: () -> Unit
        ) {
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
                                    onClick = { onArtistClick(artist.id) }
                                )
                            }
                        }
                        IconButton(
                            content = { Icon(Icons.Default.PlayCircle, null) },
                            onClick = onPlayClick
                        )
                    }
                }
            }
        }
    }
}
