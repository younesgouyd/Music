package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.Image
import dev.younesgouyd.apps.music.client.components.util.Item
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.Tag
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TagDetails(
    id: TagId,
    tagRepo: TagRepo,
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo,
    artistRepo: ArtistRepo,
    tagTrackCrossRefRepo: TagTrackCrossRefRepo,
    showTrack: (TrackId) -> Unit,
    showArtist: (ArtistId) -> Unit
) : Component() {
    override val title: String = "Tag"
    private val searchQuery = MutableStateFlow("")
    private val state: MutableStateFlow<TagDetailsState> = MutableStateFlow(TagDetailsState.Loading)

    init {
        coroutineScope.launch {
            state.value = TagDetailsState.Loaded(
                tag = tagRepo.get(id).stateIn(coroutineScope),
                scrollState = LazyListState(),
                searchQuery = searchQuery.asStateFlow(),
                tracks = searchQuery.flatMapLatest { trackRepo.searchTag(it, id) }
                    .map { dbList ->
                        dbList.map { dbTrack ->
                            TagDetailsState.Loaded.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                image = mediaFileRepo.getTrackImage(dbTrack.id),
                                artists = artistRepo.getTrackArtists(dbTrack.id).first().map { dbArtist ->
                                    TagDetailsState.Loaded.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                onSearchQueryChange = { searchQuery.value = it },
                onTrackClick = showTrack,
                onArtistClick = showArtist,
                onRemoveTrackClick = {
                    coroutineScope.launch {
                        tagTrackCrossRefRepo.delete(id, it)
                    }
                }
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

    private sealed class TagDetailsState() {
        data object Loading : TagDetailsState()

        data class Loaded(
            val tag: StateFlow<Tag>,
            val scrollState: LazyListState,
            val searchQuery: StateFlow<String>,
            val tracks: StateFlow<List<Track>>,
            val onSearchQueryChange: (String) -> Unit,
            val onTrackClick: (TrackId) -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onRemoveTrackClick: (TrackId) -> Unit
        ) : TagDetailsState() {
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
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: TagDetailsState
        ) {
            when (state) {
                is TagDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is TagDetailsState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            loaded: TagDetailsState.Loaded
        ) {
            Main(
                modifier = modifier,
                tag = loaded.tag,
                scrollState = loaded.scrollState,
                searchQuery = loaded.searchQuery,
                tracks = loaded.tracks,
                onSearchQueryChange = loaded.onSearchQueryChange,
                onTrackClick = loaded.onTrackClick,
                onArtistClick = loaded.onArtistClick,
                onRemoveTrackClick = loaded.onRemoveTrackClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            tag: StateFlow<Tag>,
            scrollState: LazyListState,
            searchQuery: StateFlow<String>,
            tracks: StateFlow<List<TagDetailsState.Loaded.Track>>,
            onSearchQueryChange: (String) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onRemoveTrackClick: (TrackId) -> Unit
        ) {
            val tag by tag.collectAsState()
            val searchQuery by searchQuery.collectAsState()
            val tracks by tracks.collectAsState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            TagInfo(
                                modifier = Modifier.fillMaxWidth(),
                                tag = tag
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                stickyHeader {
                                    Surface {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                label = { Text("Search") },
                                                value = searchQuery,
                                                onValueChange = onSearchQueryChange
                                            )
                                        }
                                    }
                                }
                                items(items = tracks, key = { it.id.value }) { track ->
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        track = track,
                                        onClick = { onTrackClick(track.id) },
                                        onArtistClick = onArtistClick,
                                        onRemoveClick = { onRemoveTrackClick(track.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        @Composable
        private fun TagInfo(
            modifier: Modifier,
            tag: Tag
        ) {
            Surface(
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.displayMedium
                    )
                    if (tag.description != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = tag.description
                        )
                    }
                }
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier,
            track: TagDetailsState.Loaded.Track,
            onClick: () -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onRemoveClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(250.dp),
                        file = track.image
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            },
                                            onClick = { onArtistClick(artist.id) }
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            content = { Icon(Icons.Default.Remove, null) },
                            onClick = onRemoveClick
                        )
                    }
                }
            }
        }
    }
}