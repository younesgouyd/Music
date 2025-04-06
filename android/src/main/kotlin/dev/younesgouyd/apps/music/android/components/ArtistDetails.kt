package dev.younesgouyd.apps.music.android.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.android.components.util.widgets.*
import dev.younesgouyd.apps.music.common.components.ArtistDetails
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ArtistDetails(
    id: Long,
    artistRepo: ArtistRepo,
    albumRepo: AlbumRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    folderRepo: FolderRepo,
    playlistRepo: PlaylistRepo,
    mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit
) : ArtistDetails(
    id, artistRepo, albumRepo, playlistTrackCrossRefRepo, trackRepo, folderRepo, playlistRepo,
    mediaController, showAlbumDetails, showArtistDetails
) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun showAddAlbumToPlaylistDialog(albumId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Album(albumId),
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
                onAddAlbumToQueueClick = state.onAddAlbumToQueueClick,
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
            onAddAlbumToQueueClick: (id: Long) -> Unit,
            onPlayAlbumClick: (Long) -> Unit
        ) {
            val artist by artist.collectAsState()
            val albumItems by albums.collectAsState()
            val lazyGridState = rememberLazyGridState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                                    onAddToQueueClick = { onAddAlbumToQueueClick(album.id) },
                                    onPlayClick = { onPlayAlbumClick(album.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ScrollToTopFloatingActionButton(
                        lazyGridState
                    )
                }
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
            onAddToQueueClick: () -> Unit,
            onPlayClick: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }

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
                    item = Item(name = album.name, image = album.image),
                    onDismiss = { showContextMenu = false }
                ) {
                    Option(
                        label = "Add to playlist",
                        icon = Icons.AutoMirrored.Default.PlaylistAdd,
                        onClick = onAddToPlaylistClick,
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
