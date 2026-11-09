package dev.younesgouyd.apps.music.client.multiplatform.components

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
import dev.younesgouyd.apps.music.client.multiplatform.components.util.Image
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagTrackCrossRefRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.Tag
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TagDetails(
    id: TagId,
    tagRepo: TagRepo,
    trackRepo: TrackRepo,
    tagTrackCrossRefRepo: TagTrackCrossRefRepo,
    showTrack: (TrackId) -> Unit,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Tag"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            val searchQuery = MutableStateFlow("")
            val tracks = searchQuery.flatMapLatest {
                trackRepo.searchWithTag(it, id) }.mapLatest { dbList ->
                dbList.map { dbTrack ->
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
            }.stateIn(coroutineScope)
            tagRepo.get(id).mapLatest { dbTag ->
                if (dbTag == null) return@mapLatest Ui.State.ItemDoesNotExist
                Ui.State.Loaded(
                    scrollState = LazyListState(),
                    tag = dbTag,
                    searchQuery = searchQuery.asStateFlow(),
                    tracks = tracks,
                    onSearchQueryChange = { searchQuery.value = it },
                    onTrackClick = showTrack,
                    onArtistClick = showArtist,
                    onRemoveTrackClick = {
                        coroutineScope.launch {
                            tagTrackCrossRefRepo.delete(id, it)
                        }
                    }
                )
            }.collect(state)
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
                val scrollState: LazyListState,
                val tag: Tag,
                val searchQuery: StateFlow<String>,
                val tracks: StateFlow<List<Track>>,
                val onSearchQueryChange: (String) -> Unit,
                val onTrackClick: (TrackId) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onRemoveTrackClick: (TrackId) -> Unit
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
        private fun Main(
            modifier: Modifier,
            loaded: State.Loaded
        ) {
            Main(
                modifier = modifier,
                scrollState = loaded.scrollState,
                tag = loaded.tag,
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
            scrollState: LazyListState,
            tag: Tag,
            searchQuery: StateFlow<String>,
            tracks: StateFlow<List<State.Loaded.Track>>,
            onSearchQueryChange: (String) -> Unit,
            onTrackClick: (TrackId) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onRemoveTrackClick: (TrackId) -> Unit
        ) {
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
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = tag.name,
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier,
            track: State.Loaded.Track,
            onClick: () -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
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
                        id = track.image
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
                                            onClick = { onArtistClick(artist.id!!) },
                                            enabled = artist.id != null
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