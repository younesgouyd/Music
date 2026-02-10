package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistList(
    artistRepo: SpotifyArtistRepo,
    mediaFileRepo: MediaFileRepo,
    mediaController: MediaController,
    showArtistDetails: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Artists"
    private val state: MutableStateFlow<ArtistListState> = MutableStateFlow(ArtistListState.Loading)
    private val searchQuery = MutableStateFlow("")

    init {
        coroutineScope.launch {
            state.value = ArtistListState.Loaded(
                artists = searchQuery.flatMapLatest { nameQuery ->
                    artistRepo.search(nameQuery).map { list ->
                        list.map { dbArtist ->
                            ArtistListState.Loaded.ArtistItem(
                                id = dbArtist.id,
                                name = dbArtist.name,
                                image = mediaFileRepo.getSpotifyArtistImage(dbArtist.id)
                            )
                        }
                    }
                }.stateIn(coroutineScope),
                searchQuery = searchQuery.asStateFlow(),
                scrollState = LazyGridState(),
                onSearchQueryChange = { searchQuery.value = it },
                onArtistClick = showArtistDetails,
                onPlayArtistClick = { id: SpotifyArtistId -> mediaController.playQueue(listOf(MediaController.QueueItemParameter.Artist(id))) },
                onAddArtistToQueueClick = { id: SpotifyArtistId -> mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Artist(id))) }
            )
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        AdaptiveUi(
            wide = { Ui.Wide.Main(modifier = modifier, state = state) },
            compact = { Ui.Compact.Main(modifier = modifier, state = state) }
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class ArtistListState {
        data object Loading : ArtistListState()

        data class Loaded(
            val artists: StateFlow<List<ArtistItem>>,
            val searchQuery: StateFlow<String>,
            val scrollState: LazyGridState,
            val onSearchQueryChange: (String) -> Unit,
            val onArtistClick: (SpotifyArtistId) -> Unit,
            val onPlayArtistClick: (SpotifyArtistId) -> Unit,
            val onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
        ) : ArtistListState() {
            data class ArtistItem(
                val id: SpotifyArtistId,
                val name: String,
                val image: File?
            )
        }
    }

    private object Ui {
        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: ArtistListState) {
                when (state) {
                    is ArtistListState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is ArtistListState.Loaded -> Main(modifier = modifier, loaded = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, loaded: ArtistListState.Loaded) {
                Main(
                    modifier = modifier,
                    artists = loaded.artists,
                    searchQuery = loaded.searchQuery,
                    scrollState = loaded.scrollState,
                    onSearchQueryChange = loaded.onSearchQueryChange,
                    onArtistClick = loaded.onArtistClick,
                    onPlayArtistClick = loaded.onPlayArtistClick,
                    onAddArtistToQueueClick = loaded.onAddArtistToQueueClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artists: StateFlow<List<ArtistListState.Loaded.ArtistItem>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyGridState,
                onSearchQueryChange: (String) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onPlayArtistClick: (SpotifyArtistId) -> Unit,
                onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
            ) {
                val items by artists.collectAsState()
                val searchQuery by searchQuery.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                columns = GridCells.Adaptive(200.dp)
                            ) {
                                stickyHeader {
                                    Surface {
                                        OutlinedTextField(
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Default.Search, null) },
                                            label = { Text("Search") },
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange
                                        )
                                    }
                                }
                                items(
                                    items = items,
                                    key = { it.id.value }
                                ) { artist ->
                                    ArtistItem(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) },
                                        onPlayClick = { onPlayArtistClick(artist.id) },
                                        onAddToQueueClick = { onAddArtistToQueueClick(artist.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )
            }

            @Composable
            private fun ArtistItem(
                modifier: Modifier = Modifier,
                artist: ArtistListState.Loaded.ArtistItem,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

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
                            file = artist.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = artist.name,
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
                            IconButton(
                                content = { Icon(Icons.Default.PlayCircle, null) },
                                onClick = onPlayClick
                            )
                            IconButton(
                                content = { Icon(Icons.Default.MoreVert, null) },
                                onClick = { showContextMenu = true }
                            )
                        }
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = artist.name,
                            image = artist.image
                        ),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueueClick,
                        )
                        Option(
                            label = "Play next",
                            icon = Icons.Default.QueuePlayNext,
                            onClick = { TODO() },
                        )
                    }
                }
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: ArtistListState) {
                when (state) {
                    is ArtistListState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is ArtistListState.Loaded -> Main(modifier = modifier, loaded = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, loaded: ArtistListState.Loaded) {
                Main(
                    modifier = modifier,
                    artists = loaded.artists,
                    searchQuery = loaded.searchQuery,
                    scrollState = loaded.scrollState,
                    onSearchQueryChange = loaded.onSearchQueryChange,
                    onArtistClick = loaded.onArtistClick,
                    onPlayArtistClick = loaded.onPlayArtistClick,
                    onAddArtistToQueueClick = loaded.onAddArtistToQueueClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artists: StateFlow<List<ArtistListState.Loaded.ArtistItem>>,
                searchQuery: StateFlow<String>,
                scrollState: LazyGridState,
                onSearchQueryChange: (String) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onPlayArtistClick: (SpotifyArtistId) -> Unit,
                onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
            ) {
                val items by artists.collectAsState()
                val searchQuery by searchQuery.collectAsState()

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                state = scrollState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                columns = GridCells.Adaptive(100.dp)
                            ) {
                                stickyHeader {
                                    Surface {
                                        OutlinedTextField(
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Default.Search, null) },
                                            label = { Text("Search") },
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange
                                        )
                                    }
                                }
                                items(
                                    items = items,
                                    key = { it.id.value }
                                ) { artist ->
                                    ArtistItem(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) },
                                        onPlayClick = { onPlayArtistClick(artist.id) },
                                        onAddToQueueClick = { onAddArtistToQueueClick(artist.id) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        ScrollToTopFloatingActionButton(scrollState)
                    }
                )
            }

            @Composable
            private fun ArtistItem(
                modifier: Modifier = Modifier,
                artist: ArtistListState.Loaded.ArtistItem,
                onClick: () -> Unit,
                onPlayClick: () -> Unit,
                onAddToQueueClick: () -> Unit
            ) {
                var showContextMenu by remember { mutableStateOf(false) }

                Item(
                    modifier = modifier,
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.aspectRatio(1f),
                            file = artist.image,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showContextMenu) {
                    ItemContextMenu(
                        item = Item(
                            name = artist.name,
                            image = artist.image
                        ),
                        onDismiss = { showContextMenu = false }
                    ) {
                        Option(
                            label = "Play",
                            icon = Icons.Default.PlayCircle,
                            onClick = { onPlayClick(); showContextMenu = false },
                        )
                        Option(
                            label = "Add to queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = onAddToQueueClick,
                        )
                        Option(
                            label = "Play next",
                            icon = Icons.Default.QueuePlayNext,
                            onClick = { TODO() },
                        )
                    }
                }
            }
        }
    }
}
