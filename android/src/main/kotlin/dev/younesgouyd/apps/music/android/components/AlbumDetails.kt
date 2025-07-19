package dev.younesgouyd.apps.music.android.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import dev.younesgouyd.apps.music.common.components.AlbumDetails
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AlbumDetails(
    id: Long,
    albumRepo: AlbumRepo,
    artistRepo: ArtistRepo,
    trackRepo: TrackRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    playlistRepo: PlaylistRepo,
    folderRepo: FolderRepo,
    mediaController: MediaController,
    showArtistDetails: (Long) -> Unit
) : AlbumDetails(
    id, albumRepo, artistRepo, trackRepo, playlistTrackCrossRefRepo, playlistRepo, folderRepo,
    mediaController, showArtistDetails
) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun showAddToPlaylistDialog() {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Album(id),
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

    override fun showAddTrackToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Track(trackId),
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
        fun Main(modifier: Modifier, state: AlbumDetailsState) {
            when (state) {
                is AlbumDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AlbumDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: AlbumDetailsState.Loaded) {
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier = modifier,
                album = state.album,
                tracks = state.tracks,
                onArtistClick = state.onArtistClick,
                onPlayClick = state.onPlayClick,
                onAddToQueueClick = state.onAddToQueueClick,
                onAddToPlaylistClick = state.onAddToPlaylistClick,
                onTrackClick = state.onTrackClick,
                onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                onAddTrackToQueue = state.onAddTrackToQueue
            )

            if (addToPlaylistDialogVisible) {
                Dialog(onDismissRequest = state.onDismissAddToPlaylistDialog) {
                    addToPlaylist!!.show(Modifier)
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            album: StateFlow<AlbumDetailsState.Loaded.Album>,
            tracks: StateFlow<List<AlbumDetailsState.Loaded.Album.Track>>,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onTrackClick: (Long) -> Unit,
            onAddTrackToPlaylistClick: (id: Long) -> Unit,
            onAddTrackToQueue: (id: Long) -> Unit
        ) {
            val album by album.collectAsState()
            val items by tracks.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyColumnState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                AlbumInfo(
                                    modifier = Modifier.fillMaxWidth(),
                                    album = album,
                                    onArtistClick = onArtistClick
                                )
                            }
                            item {
                                Actions(
                                    modifier = Modifier.fillMaxWidth(),
                                    onPlayClick = onPlayClick,
                                    onAddToQueueClick = onAddToQueueClick,
                                    onAddToPlaylistClick = onAddToPlaylistClick
                                )
                            }
                            item {
                                Spacer(Modifier.size(8.dp))
                            }
                            stickyHeader {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Tracks",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Start
                                )
                                HorizontalDivider()
                            }
                            items(items = items, key = { it.id }) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    track = track,
                                    albumImage = album.image,
                                    onClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                    onAddToQueue = { onAddTrackToQueue(track.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ScrollToTopFloatingActionButton(
                        lazyColumnState
                    )
                }
            )
        }

        @Composable
        private fun AlbumInfo(
            modifier: Modifier,
            album: AlbumDetailsState.Loaded.Album,
            onArtistClick: (Long) -> Unit,
        ) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    data = album.image,
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = album.name,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.releaseDate != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Released: ${album.releaseDate}",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(album.artists) { artist ->
                        TextButton(
                            onClick = { onArtistClick(artist.id) },
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    content = {
                                        Icon(Icons.Default.Person, null)
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        @Composable
        private fun Actions(
            modifier: Modifier,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit
        ) {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    IconButton(
                        onClick = onPlayClick
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onAddToQueueClick
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddToQueue, null)
                            Text(text = "Add to queue", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onAddToPlaylistClick
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Default.PlaylistAdd, null)
                            Text(text = "Add to playlist", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        private const val TITLE_WEIGHT = .9f
        private const val ACTIONS_WEIGHT = .1f

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            track: AlbumDetailsState.Loaded.Album.Track,
            albumImage: ByteArray?,
            onClick: () -> Unit,
            onArtistClick: (Long) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onAddToQueue: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }

            Row(
                modifier = modifier.clickable(onClick = onClick),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.CenterStart) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = track.artists, key = { it.id }) { artist ->
                                TextButton(
                                    onClick = { onArtistClick(artist.id) },
                                    content = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, null)
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.CenterEnd) {
                    IconButton(
                        content = { Icon(Icons.Default.MoreVert, null) },
                        onClick = { showContextMenu = true }
                    )
                }
            }
            HorizontalDivider()

            if (showContextMenu) {
                ItemContextMenu(
                    item = Item(name = track.name, image = albumImage),
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
                        onClick = onAddToQueue,
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