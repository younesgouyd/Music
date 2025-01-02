package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.Item
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetails(
    private val id: Long,
    private val artistRepo: ArtistRepo,
    private val albumRepo: AlbumRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val showAlbumDetails: (Long) -> Unit,
    private val showArtistDetails: (Long) -> Unit,
    playAlbum: (Long) -> Unit
) : Component() {
    override val title: String = "Artist"
    private val state: MutableStateFlow<ArtistDetailsState> = MutableStateFlow(ArtistDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                ArtistDetailsState.Loaded(
                    artist = artistRepo.get(id).mapLatest { dbArtist ->
                        ArtistDetailsState.Loaded.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name,
                            image = dbArtist.image
                        )
                    }.stateIn(coroutineScope),
                    albums = albumRepo.getArtistAlbums(id).mapLatest { list ->
                        list.map { dbAlbum ->
                            ArtistDetailsState.Loaded.Artist.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date,
                                artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                    ArtistDetailsState.Loaded.Artist.Album.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onAlbumClick = showAlbumDetails,
                    onArtistClick = showArtistDetails,
                    onPlayAlbumClick = playAlbum,
                    onAddAlbumToPlaylistClick = ::showAddAlbumToPlaylistDialog,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
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

    private fun showAddAlbumToPlaylistDialog(albumId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Album(albumId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                albumRepo = albumRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.update { true }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    private sealed class ArtistDetailsState {
        data object Loading : ArtistDetailsState()

        data class Loaded(
            val artist: StateFlow<Artist>,
            val albums: StateFlow<List<Artist.Album>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onPlayAlbumClick: (Long) -> Unit,
            val onAddAlbumToPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : ArtistDetailsState() {
            data class Artist(
                val id: Long,
                val name: String,
                val image: ByteArray?
            ) {
                data class Track(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?
                )

                data class Album(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?,
                    val releaseDate: String?,
                    val artists: List<Artist>
                ) {
                    data class Artist(
                        val id: Long,
                        val name: String
                    )
                }
            }
        }
    }
    
    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: ArtistDetailsState) {
            when (state) {
                is ArtistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is ArtistDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: ArtistDetailsState.Loaded) {
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier = modifier,
                artist = state.artist,
                albums = state.albums,
                onAlbumClick = state.onAlbumClick,
                onArtistClick = state.onArtistClick,
                onAddAlbumToPlaylistClick = state.onAddAlbumToPlaylistClick,
                onPlayAlbumClick = state.onPlayAlbumClick,
            )

            if (addToPlaylistDialogVisible) {
                Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                    addToPlaylist!!.show(Modifier)
                }
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            artist: StateFlow<ArtistDetailsState.Loaded.Artist>,
            albums: StateFlow<List<ArtistDetailsState.Loaded.Artist.Album>>,
            onAlbumClick: (Long) -> Unit,
            onArtistClick: (Long) -> Unit,
            onAddAlbumToPlaylistClick: (id: Long) -> Unit,
            onPlayAlbumClick: (Long) -> Unit
        ) {
            val artist by artist.collectAsState()
            val albumItems by albums.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        VerticalScrollbar(lazyGridState)
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyGridState,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            columns = GridCells.Adaptive(200.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ArtistInfo(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    artist = artist,
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Discography",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            items(
                                items = albumItems,
                                key = { it.id }
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) },
                                    onArtistClick = onArtistClick,
                                    onAddToPlaylistClick = { onAddAlbumToPlaylistClick(album.id) },
                                    onPlayClick = { onPlayAlbumClick(album.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyGridState) }
            )
        }

        @Composable
        private fun ArtistInfo(
            modifier: Modifier = Modifier,
            artist: ArtistDetailsState.Loaded.Artist
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    data = artist.image,
                    contentScale = ContentScale.FillHeight
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = artist.name,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        @Composable
        private fun AlbumItem(
            modifier: Modifier = Modifier,
            album: ArtistDetailsState.Loaded.Artist.Album,
            onClick: () -> Unit,
            onArtistClick: (Long) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onPlayClick: () -> Unit
        ) {
            Item (
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
                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, null) },
                            onClick = onAddToPlaylistClick
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
