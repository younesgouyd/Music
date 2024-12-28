package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
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
import dev.younesgouyd.apps.music.app.components.AlbumDetails.AlbumDetailsState.Loaded.Album
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.app.components.util.widgets.VerticalScrollbar
import dev.younesgouyd.apps.music.app.data.repoes.AlbumRepo
import dev.younesgouyd.apps.music.app.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.app.data.repoes.TrackRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetails(
    private val id: Long,
    private val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    private val trackRepo: TrackRepo,
    private val showArtistDetails: (Long) -> Unit,
    play: () -> Unit,
    playTrack: (Long) -> Unit
) : Component() {
    override val title: String = "Album"
    private val state: MutableStateFlow<AlbumDetailsState> = MutableStateFlow(AlbumDetailsState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                AlbumDetailsState.Loaded(
                    album = albumRepo.get(id).mapLatest { dbAlbum ->
                        AlbumDetailsState.Loaded.Album(
                            id = dbAlbum.id,
                            name = dbAlbum.name,
                            artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                AlbumDetailsState.Loaded.Album.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name
                                )
                            },
                            image = dbAlbum.image,
                            releaseDate = dbAlbum.release_date
                        )
                    }.stateIn(coroutineScope),
                    tracks = trackRepo.getAlbumTracks(id).mapLatest { list ->
                        list.map { dbTrack ->
                            AlbumDetailsState.Loaded.Album.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    AlbumDetailsState.Loaded.Album.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    onArtistClick = showArtistDetails,
                    onPlayClick = play,
                    onTrackClick = playTrack
                )
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class AlbumDetailsState {
        data object Loading : AlbumDetailsState()

        data class Loaded(
            val album: StateFlow<Album>,
            val tracks: StateFlow<List<Album.Track>>,
            val onArtistClick: (Long) -> Unit,
            val onPlayClick: () -> Unit,
            val onTrackClick: (Long) -> Unit
        ) : AlbumDetailsState() {
            data class Album(
                val id: Long,
                val name: String,
                val artists: List<Artist>,
                val image: ByteArray?,
                val releaseDate: String?,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )

                data class Track(
                    val id: Long,
                    val name: String,
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
        fun Main(modifier: Modifier, state: AlbumDetailsState) {
            when (state) {
                is AlbumDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AlbumDetailsState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: AlbumDetailsState.Loaded) {
            Main(
                modifier = modifier,
                album = loaded.album,
                tracks = loaded.tracks,
                onArtistClick = loaded.onArtistClick,
                onPlayClick = loaded.onPlayClick,
                onTrackClick = loaded.onTrackClick
            )
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            album: StateFlow<Album>,
            tracks: StateFlow<List<Album.Track>>,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit,
            onTrackClick: (Long) -> Unit,
        ) {
            val album by album.collectAsState()
            val items by tracks.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        VerticalScrollbar(lazyColumnState)
                        LazyColumn (
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = lazyColumnState,
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                AlbumInfo(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    album = album,
                                    onArtistClick = onArtistClick,
                                    onPlayClick = onPlayClick,
                                )
                            }
                            item {
                                Spacer(Modifier.size(8.dp))
                            }
                            stickyHeader {
                                TracksHeader(modifier = Modifier.fillMaxWidth().height(64.dp))
                                HorizontalDivider()
                            }
                            items(items = items, key = { it.id }) { track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    track = track,
                                    onClick = { onTrackClick(track.id) },
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(lazyColumnState) }
            )
        }

        @Composable
        private fun AlbumInfo(
            modifier: Modifier,
            album: Album,
            onArtistClick: (Long) -> Unit,
            onPlayClick: () -> Unit
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    url = null,
                    contentScale = ContentScale.FillHeight
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = album.name,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (artist in album.artists) {
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
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                    if (album.releaseDate != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Release date: ${album.releaseDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
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

        private const val TITLE_WEIGHT = .9f
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
            track: Album.Track,
            onClick: () -> Unit,
            onArtistClick: (Long) -> Unit
        ) {
            Row(
                modifier = modifier.clickable { onClick() },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // image + title + artists
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
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
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