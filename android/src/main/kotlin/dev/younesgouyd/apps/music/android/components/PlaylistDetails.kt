package dev.younesgouyd.apps.music.android.components

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
import dev.younesgouyd.apps.music.common.components.PlaylistDetails
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PlaylistDetails(
    id: Long,
    trackRepo: TrackRepo,
    playlistRepo: PlaylistRepo,
    artistRepo: ArtistRepo,
    albumRepo: AlbumRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    folderRepo: FolderRepo,
    mediaController: MediaController,
    showArtistDetails: (id: Long) -> Unit,
    showAlbumDetails: (id: Long) -> Unit
) : PlaylistDetails(
    id, trackRepo, playlistRepo, artistRepo, albumRepo, playlistTrackCrossRefRepo, folderRepo,
    mediaController, showArtistDetails, showAlbumDetails
) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun showAddToPlaylistDialog() {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Playlist(id),
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
        fun Main(modifier: Modifier, state: PlaylistDetailsState) {
            when (state) {
                is PlaylistDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is PlaylistDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: PlaylistDetailsState.Loaded) {
            val addToPlaylistDialogVisible by state.addToPlaylistDialogVisible.collectAsState()
            val addToPlaylist by state.addToPlaylist.collectAsState()

            Main(
                modifier = modifier,
                playlist = state.playlist,
                tracks = state.tracks,
                onPlayClick = state.onPlayClick,
                onAddToQueueClick = state.onAddToQueueClick,
                onAddToPlaylistClick = state.onAddToPlaylistClick,
                onTrackClick = state.onTrackClick,
                onArtistClick = state.onArtistClick,
                onAlbumClick = state.onAlbumClick,
                onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick,
                onAddTrackToQueueClick = state.onAddTrackToQueueClick,
                onRemoveTrackFromPlaylistClick = state.onRemoveTrackFromPlaylistClick
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
            playlist: StateFlow<PlaylistDetailsState.Loaded.Playlist>,
            tracks: StateFlow<List<PlaylistDetailsState.Loaded.Track>>,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onTrackClick: (id: Long) -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onAlbumClick: (id: Long) -> Unit,
            onAddTrackToPlaylistClick: (id: Long) -> Unit,
            onAddTrackToQueueClick: (id: Long) -> Unit,
            onRemoveTrackFromPlaylistClick: (id: Long) -> Unit
        ) {
            val playlist by playlist.collectAsState()
            val items by tracks.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = {
                    Box(modifier = Modifier.fillMaxSize().padding(it)) {
                        LazyColumn (
                            modifier = Modifier.fillMaxSize(),
                            state = lazyColumnState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                PlaylistInfo(
                                    modifier = Modifier.fillMaxWidth(),
                                    playlist = playlist,
                                    onPlayClick = onPlayClick,
                                    onAddToQueueClick = onAddToQueueClick,
                                    onAddToPlaylistClick = onAddToPlaylistClick
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
                            items(items = items) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    track = track,
                                    onTrackClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) },
                                    onAddToQueueClick = { onAddTrackToQueueClick(track.id) },
                                    onRemoveFromPlaylistClick = { onRemoveTrackFromPlaylistClick(track.id) }
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
        private fun PlaylistInfo(
            modifier: Modifier,
            playlist: PlaylistDetailsState.Loaded.Playlist,
            onPlayClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onAddToPlaylistClick: () -> Unit
        ) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    data = playlist.image,
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = playlist.name,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
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

        private const val TITLE_WEIGHT = .45f
        private const val ACTIONS_WEIGHT = .1f

//        @Composable
//        private fun TracksHeader(modifier: Modifier = Modifier) {
//            Surface {
//                Row(
//                    modifier = modifier,
//                    horizontalArrangement = Arrangement.Start,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.Center) {
//                        Text(
//                            text = "Title",
//                            style = MaterialTheme.typography.titleMedium,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Spacer(Modifier.width(8.dp))
//                    Box(modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT), contentAlignment = Alignment.Center) {
//                        Text(
//                            text = "Album",
//                            style = MaterialTheme.typography.titleMedium,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Spacer(Modifier.width(8.dp))
//                    Box(modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT), contentAlignment = Alignment.Center) {
//                        Text(
//                            text = "Date added",
//                            style = MaterialTheme.typography.titleMedium,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Spacer(Modifier.width(8.dp))
//                    Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.Center) {
//                        Text(
//                            text = "",
//                            style = MaterialTheme.typography.titleMedium,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                }
//            }
//        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            track: PlaylistDetailsState.Loaded.Track,
            onTrackClick: () -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onAlbumClick: (id: Long) -> Unit,
            onAddToPlaylistClick: () -> Unit,
            onAddToQueueClick: () -> Unit,
            onRemoveFromPlaylistClick: () -> Unit
        ) {
            var showContextMenu by remember { mutableStateOf(false) }

            Row(
                modifier = modifier.clickable { onTrackClick() },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.CenterStart) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.fillMaxHeight(),
                            data = track.album?.image,
                            contentScale = ContentScale.FillHeight
                        )
                        Column(
                            modifier = Modifier.weight(1f),
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
                    item = Item(name = track.name, image = track.album?.image),
                    onDismiss = { showContextMenu = false }
                ) {
                    Option(
                        label = "Remove from playlist",
                        icon = Icons.Default.Remove,
                        onClick = { onRemoveFromPlaylistClick(); showContextMenu = false }
                    )
                    Option(
                        label = "Add to playlist",
                        icon = Icons.AutoMirrored.Default.PlaylistAdd,
                        onClick = onAddToPlaylistClick
                    )
                    Option(
                        label = "Add to queue",
                        icon = Icons.Default.AddToQueue,
                        onClick = { onAddToQueueClick(); showContextMenu = false }
                    )
                    Option(
                        label = "Play next",
                        icon = Icons.Default.QueuePlayNext,
                        onClick = { TODO() }
                    )
                }
            }
        }
    }
}
