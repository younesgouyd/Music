package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
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
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.Item
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.AlbumRepo
import dev.younesgouyd.apps.music.app.data.repoes.ArtistRepo
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlbumList(
    albumRepo: AlbumRepo,
    artistRepo: ArtistRepo,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit,
    playAlbum: (Long) -> Unit,
    addAlbumToQueue: (Long) -> Unit
) : Component() {
    override val title: String = "Albums"
    private val state: MutableStateFlow<AlbumListState> = MutableStateFlow(AlbumListState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                AlbumListState.Loaded(
                    albums = albumRepo.getAll().mapLatest { list ->
                        list.map { dbAlbum ->
                            AlbumListState.Loaded.AlbumListItem(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date,
                                artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                    AlbumListState.Loaded.AlbumListItem.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    onAlbumClick = showAlbumDetails,
                    onArtistClick = showArtistDetails,
                    onPlayAlbumClick = playAlbum,
                    onAddAlbumToQueueClick = addAlbumToQueue
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

    private sealed class AlbumListState {
        data object Loading : AlbumListState()

        data class Loaded(
            val albums: StateFlow<List<AlbumListItem>>,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onPlayAlbumClick: (Long) -> Unit,
            val onAddAlbumToQueueClick: (Long) -> Unit
        ) : AlbumListState() {
            data class AlbumListItem(
                val id: Long,
                val name: String,
                val image: ByteArray?,
                val releaseDate: String?,
                val artists: List<Artist>,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )
            }
        }
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: AlbumListState) {
            when (state) {
                is AlbumListState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AlbumListState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: AlbumListState.Loaded) {
            Main(
                modifier = modifier,
                albums = loaded.albums,
                onAlbumClick = loaded.onAlbumClick,
                onArtistClick = loaded.onArtistClick,
                onPlayAlbumClick = loaded.onPlayAlbumClick,
                onAddAlbumToQueueClick = loaded.onAddAlbumToQueueClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            albums: StateFlow<List<AlbumListState.Loaded.AlbumListItem>>,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onPlayAlbumClick: (Long) -> Unit,
            onAddAlbumToQueueClick: (Long) -> Unit
        ) {
            val items by albums.collectAsState()
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
                            items(items = items, key = { it.id }) { album ->
                                AlbumItem(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) },
                                    onArtistClick = onArtistClick,
                                    onPlayClick = { onPlayAlbumClick(album.id) },
                                    onAddToQueueClick = { onAddAlbumToQueueClick(album.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) }
            )
        }

        @Composable
        private fun AlbumItem(
            modifier: Modifier = Modifier,
            album: AlbumListState.Loaded.AlbumListItem,
            onClick: () -> Unit,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.aspectRatio(1f),
                        data = album.image,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = album.releaseDate?.let { "Released: $it" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(items = album.artists) { artist ->
                            TextButton(
                                onClick = { onArtistClick(artist.id) },
                                content = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Person, null)
                                        Text(artist.name)
                                    }
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            content = { Icon(Icons.Default.AddToQueue, null) },
                            onClick = onAddToQueueClick
                        )
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