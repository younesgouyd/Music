package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.Item
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.ArtistRepo
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArtistList(
    artistRepo: ArtistRepo,
    showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Artists"
    private val state: MutableStateFlow<ArtistListState> = MutableStateFlow(ArtistListState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                ArtistListState.Loaded(
                    artists = artistRepo.getAll().mapLatest { list ->
                        list.map { dbArtist ->
                            ArtistListState.Loaded.ArtistItem(
                                id = dbArtist.id,
                                name = dbArtist.name,
                                image = dbArtist.image
                            )
                        }
                    }.stateIn(coroutineScope),
                    onArtistClick = showArtistDetails
                )
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class ArtistListState {
        data object Loading : ArtistListState()

        data class Loaded(
            val artists: StateFlow<List<ArtistItem>>,
            val onArtistClick: (Long) -> Unit
        ) : ArtistListState() {
            data class ArtistItem(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )
        }
    }

    private object Ui {
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
                onArtistClick = loaded.onArtistClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            artists: StateFlow<List<ArtistListState.Loaded.ArtistItem>>,
            onArtistClick: (Long) -> Unit
        ) {
            val items by artists.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        VerticalScrollbar(lazyGridState)
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyGridState,
                            contentPadding = PaddingValues(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            columns = GridCells.Adaptive(200.dp)
                        ) {
                            items(
                                items = items,
                                key = { it.id }
                            ) { item ->
                                ArtistItem(
                                    artist = item,
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) }
            )
        }

        @Composable
        private fun ArtistItem(
            modifier: Modifier = Modifier,
            artist: ArtistListState.Loaded.ArtistItem,
            onArtistClick: (Long) -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = { onArtistClick(artist.id) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        data = artist.image,
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
                }
            }
        }
    }
}
