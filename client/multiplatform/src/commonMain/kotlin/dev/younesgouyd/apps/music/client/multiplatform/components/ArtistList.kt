package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.MediaController
import dev.younesgouyd.apps.music.client.multiplatform.components.ArtistList.Ui.State.Loaded.ArtistItem
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.QueueItemParameter
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class ArtistList(
    artistRepo: SpotifyArtistRepo,
    mediaController: MediaController,
    showArtistDetails: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Artists"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        val searchQuery = MutableStateFlow("")
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                artists = LazilyLoadedItems(
                    scope = coroutineScope,
                    loader = searchQuery.debounce(2.seconds)
                        .map { Loader(artistRepo, it) }
                        .stateIn(coroutineScope),
                    initialOffset = Offset.Id.initial<SpotifyArtistId>()
                ),
                searchQuery = searchQuery.asStateFlow(),
                onSearchQueryChange = { searchQuery.value = it },
                onArtistClick = showArtistDetails,
                onPlayArtistClick = { id: SpotifyArtistId -> mediaController.playQueue(listOf(QueueItemParameter.Artist(id))) },
                onAddArtistToQueueClick = { id: SpotifyArtistId -> mediaController.addToQueue(listOf(QueueItemParameter.Artist(id))) }
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

    private class Loader(
        private val artistRepo: SpotifyArtistRepo,
        private val query: String
    ) : LazilyLoadedItems.Loader<ArtistItem, Offset.Id<SpotifyArtistId>> {
        override suspend fun load(
            offset: Offset.Id<SpotifyArtistId>,
            pageSize: PageSize
        ): LazilyLoadedItems.Page<ArtistItem, Offset.Id<SpotifyArtistId>> {
            val rows = artistRepo.search(query, pageSize, offset)
            return LazilyLoadedItems.Page(
                nextOffset = rows.lastOrNull()?.id?.let { Offset.Id(it) },
                items = rows.map { dbArtist ->
                    ArtistItem(
                        id = dbArtist.id,
                        name = dbArtist.name,
                        image = dbArtist.mediumImgId
                    )
                }

            )
        }
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            class Loaded(
                val artists: LazilyLoadedItems<ArtistItem, Offset.Id<SpotifyArtistId>>,
                val searchQuery: StateFlow<String>,
                val onSearchQueryChange: (String) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onPlayArtistClick: (SpotifyArtistId) -> Unit,
                val onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
            ) : State() {
                data class ArtistItem(
                    val id: SpotifyArtistId,
                    val name: String,
                    val image: MediaFileId?
                )
            }
        }

        object Wide {
            @Composable
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Text(modifier = modifier, text = "Loading...")
                    is State.Loaded -> Main(modifier = modifier, loaded = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, loaded: State.Loaded) {
                Main(
                    modifier = modifier,
                    artists = loaded.artists,
                    searchQuery = loaded.searchQuery,
                    onSearchQueryChange = loaded.onSearchQueryChange,
                    onArtistClick = loaded.onArtistClick,
                    onPlayArtistClick = loaded.onPlayArtistClick,
                    onAddArtistToQueueClick = loaded.onAddArtistToQueueClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artists: LazilyLoadedItems<ArtistItem, Offset.Id<SpotifyArtistId>>,
                searchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onPlayArtistClick: (SpotifyArtistId) -> Unit,
                onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
            ) {
                val items by artists.items.collectAsState()
                val loadingItems by artists.loading.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val scrollState = rememberLazyGridState()

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
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )

                LaunchedEffect(scrollState, artists) {
                    snapshotFlow {
                        scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    }.map { it == null ||  it >= (items.size + 1) - 5  }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect { artists.loadMore() }
                }
            }

            @Composable
            private fun ArtistItem(
                modifier: Modifier = Modifier,
                artist: State.Loaded.ArtistItem,
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
                            id = artist.image,
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
                            onClick = { onAddToQueueClick(); showContextMenu = false },
                        )
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }
        }

        object Compact {
            @Composable
            fun Main(modifier: Modifier, state: State) {
                when (state) {
                    is State.Loading -> Text(modifier = modifier, text = "Loading...")
                    is State.Loaded -> Main(modifier = modifier, loaded = state)
                }
            }

            @Composable
            private fun Main(modifier: Modifier, loaded: State.Loaded) {
                Main(
                    modifier = modifier,
                    artists = loaded.artists,
                    searchQuery = loaded.searchQuery,
                    onSearchQueryChange = loaded.onSearchQueryChange,
                    onArtistClick = loaded.onArtistClick,
                    onPlayArtistClick = loaded.onPlayArtistClick,
                    onAddArtistToQueueClick = loaded.onAddArtistToQueueClick
                )
            }

            @Composable
            private fun Main(
                modifier: Modifier,
                artists: LazilyLoadedItems<ArtistItem, Offset.Id<SpotifyArtistId>>,
                searchQuery: StateFlow<String>,
                onSearchQueryChange: (String) -> Unit,
                onArtistClick: (SpotifyArtistId) -> Unit,
                onPlayArtistClick: (SpotifyArtistId) -> Unit,
                onAddArtistToQueueClick: (SpotifyArtistId) -> Unit
            ) {
                val items by artists.items.collectAsState()
                val loadingItems by artists.loading.collectAsState()
                val searchQuery by searchQuery.collectAsState()
                val scrollState = rememberLazyGridState()

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
                    floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
                )

                LaunchedEffect(scrollState, artists) {
                    snapshotFlow {
                        scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    }.map { it == null ||  it >= (items.size + 1) - 5  }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect { artists.loadMore() }
                }
            }

            @Composable
            private fun ArtistItem(
                modifier: Modifier = Modifier,
                artist: State.Loaded.ArtistItem,
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
                            id = artist.image,
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
                            onClick = { onAddToQueueClick(); showContextMenu = false },
                        )
//                        Option(
//                            label = "Play next",
//                            icon = Icons.Default.QueuePlayNext,
//                            onClick = { TODO() },
//                        )
                    }
                }
            }
        }
    }
}
