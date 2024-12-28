package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.AlbumRepo
import dev.younesgouyd.apps.music.app.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.app.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.app.data.repoes.TrackRepo
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
    showArtistDetails: (id: Long) -> Unit,
    showAlbumDetails: (id: Long) -> Unit,
    play: () -> Unit,
    playTrack: (id: Long) -> Unit,
) : Component() {
    override val title: String = "Playlist"
    private val state: MutableStateFlow<PlaylistDetailsState> = MutableStateFlow(PlaylistDetailsState.Loading)

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
                                    albumRepo.getStatic(it).let { dbAlbum ->
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
                    }.stateIn(coroutineScope),
                    onPlayClick = play,
                    onTrackClick = playTrack,
                    onArtistClick = showArtistDetails,
                    onAlbumClick = showAlbumDetails
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

    private sealed class PlaylistDetailsState {
        data object Loading : PlaylistDetailsState()

        data class Loaded(
            val playlist: StateFlow<Playlist>,
            val tracks: StateFlow<List<Track>>,
            val onPlayClick: () -> Unit,
            val onTrackClick: (id: Long) -> Unit,
            val onArtistClick: (id: Long) -> Unit,
            val onAlbumClick: (id: Long) -> Unit
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
                is PlaylistDetailsState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: PlaylistDetailsState.Loaded) {
            Main(
                modifier = modifier,
                playlist = loaded.playlist,
                tracks = loaded.tracks,
                onPlayClick = loaded.onPlayClick,
                onTrackClick = loaded.onTrackClick,
                onArtistClick = loaded.onArtistClick,
                onAlbumClick = loaded.onAlbumClick
            )
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            playlist: StateFlow<PlaylistDetailsState.Loaded.Playlist>,
            tracks: StateFlow<List<PlaylistDetailsState.Loaded.Track>>,
            onPlayClick: () -> Unit,
            onTrackClick: (id: Long) -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onAlbumClick: (id: Long) -> Unit
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
                                    onPlayClick = onPlayClick
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
                                    onTrackClick = onTrackClick,
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick
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
            onPlayClick: () -> Unit
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    url = null, // TODO
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
            onTrackClick: (id: Long) -> Unit,
            onArtistClick: (id: Long) -> Unit,
            onAlbumClick: (id: Long) -> Unit
        ) {
            Row(
                modifier = modifier.clickable { onTrackClick(track.id) },
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
                            url = null, // TODO
                            contentScale = ContentScale.FillHeight
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = track.name ?: "",
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
                        text = track.addedAt ?: "",
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
                            content = { Icon(Icons.Default.Save, null) },
                            onClick = { /* TODO: addToPlaylistDialogVisible */ }
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
