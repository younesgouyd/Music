package dev.younesgouyd.apps.music.app.components

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
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

class PlaylistDetails(
    private val id: Long,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val artistRepo: ArtistRepo,
    private val albumRepo: AlbumRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val folderRepo: FolderRepo,
    showArtistDetails: (id: Long) -> Unit,
    showAlbumDetails: (id: Long) -> Unit,
    play: () -> Unit,
    addToQueue: () -> Unit,
    playTrack: (id: Long) -> Unit
) : Component() {
    override val title: String = "Playlist"
    private val state: MutableStateFlow<PlaylistDetailsState> = MutableStateFlow(PlaylistDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                PlaylistDetailsState.Loaded(
                    playlist = playlistRepo.get(id).mapLatest { dbPlaylist ->
                        PlaylistDetailsState.Loaded.Playlist(
                            id = dbPlaylist.id,
                            name = dbPlaylist.name,
                            image = dbPlaylist.image
                        )
                    }.stateIn(coroutineScope),
                    tracks = trackRepo.getPlaylistTracks(id).mapLatest {
                        it.map { dbTrack ->
                            PlaylistDetailsState.Loaded.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    PlaylistDetailsState.Loaded.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                },
                                album = dbTrack.album_id?.let {
                                    albumRepo.getStatic(it)!!.let { dbAlbum ->
                                        PlaylistDetailsState.Loaded.Track.Album(
                                            id = dbAlbum.id,
                                            name = dbAlbum.name,
                                            image = dbAlbum.image
                                        )
                                    }
                                },
                                addedAt = Instant.ofEpochMilli(dbTrack.added_at).toString()
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onPlayClick = play,
                    onAddToQueueClick = addToQueue,
                    onTrackClick = playTrack,
                    onArtistClick = showArtistDetails,
                    onAlbumClick = showAlbumDetails,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
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

    private fun showAddToPlaylistDialog() {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Playlist(id),
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

    private fun showAddTrackToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Track(trackId),
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

    private sealed class PlaylistDetailsState {
        data object Loading : PlaylistDetailsState()

        data class Loaded(
            val playlist: StateFlow<Playlist>,
            val tracks: StateFlow<List<Track>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onPlayClick: () -> Unit,
            val onTrackClick: (id: Long) -> Unit,
            val onArtistClick: (id: Long) -> Unit,
            val onAlbumClick: (id: Long) -> Unit,
            val onAddToPlaylistClick: () -> Unit,
            val onAddToQueueClick: () -> Unit,
            val onAddTrackToPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : PlaylistDetailsState() {
            data class Playlist(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )

            data class Track(
                val id: Long,
                val name: String,
                val artists: List<Artist>,
                val album: Album?,
                val addedAt: String
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )

                data class Album(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?
                )
            }
        }
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
                onAddTrackToPlaylistClick = state.onAddTrackToPlaylistClick
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
            onAddTrackToPlaylistClick: (id: Long) -> Unit
        ) {
            val playlist by playlist.collectAsState()
            val items by tracks.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = {
                    Box(modifier = Modifier.fillMaxSize().padding(it)) {
                        VerticalScrollbar(lazyColumnState)
                        LazyColumn (
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyColumnState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                PlaylistInfo(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    playlist = playlist,
                                    onPlayClick = onPlayClick,
                                    onAddToQueueClick = onAddToQueueClick,
                                    onAddToPlaylistClick = onAddToPlaylistClick
                                )
                            }
                            item {
                                Spacer(Modifier.size(8.dp))
                            }
                            stickyHeader {
                                TracksHeader(modifier = Modifier.fillMaxWidth().height(64.dp))
                                HorizontalDivider()
                            }
                            items(items = items) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    track = track,
                                    onTrackClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    onAddToPlaylistClick = { onAddTrackToPlaylistClick(track.id) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyColumnState) }
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
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    data = playlist.image,
                    contentScale = ContentScale.FillHeight
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = playlist.name,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayCircle, null)
                                    Text(text = "Play", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onPlayClick
                        )
                        OutlinedButton(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddToQueue, null)
                                    Text(text = "Add to queue", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onAddToQueueClick
                        )
                        OutlinedButton(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Default.PlaylistAdd, null)
                                    Text(text = "Add to playlist", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onAddToPlaylistClick
                        )
                    }
                }
            }
        }

        private const val TITLE_WEIGHT = .45f
        private const val ALBUM_WEIGHT = .18f
        private const val ADDED_AT_WEIGHT = .1f
        private const val ACTIONS_WEIGHT = .1f

        @Composable
        private fun TracksHeader(modifier: Modifier = Modifier) {
            Surface {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxSize().weight(TITLE_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Album",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Date added",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.Center) {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            track: PlaylistDetailsState.Loaded.Track,
            onTrackClick: () -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onAlbumClick: (id: Long) -> Unit,
            onAddToPlaylistClick: () -> Unit
        ) {
            Row(
                modifier = modifier.clickable { onTrackClick() },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // image + title + artists
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
                                                    style = MaterialTheme.typography.bodyMedium
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

                // album
                Box(modifier = Modifier.fillMaxSize().weight(ALBUM_WEIGHT), contentAlignment = Alignment.CenterStart) {
                    if (track.album != null) {
                        TextButton(
                            onClick = { onAlbumClick(track.album.id) },
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Album, null)
                                    Text(
                                        text = track.album.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // added at
                Box(modifier = Modifier.fillMaxSize().weight(ADDED_AT_WEIGHT), contentAlignment = Alignment.Center) {
                    Text(
                        text = track.addedAt,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // actions
                Box(modifier = Modifier.fillMaxSize().weight(ACTIONS_WEIGHT), contentAlignment = Alignment.Center) {
                    Row {
                        IconButton(
                            content = { Icon(Icons.AutoMirrored.Default.PlaylistAdd, null) },
                            onClick = onAddToPlaylistClick
                        )
                        IconButton(
                            content = { Icon(Icons.Default.Folder, null) },
                            onClick = { /* TODO: addToFolderDialogVisible */ }
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
