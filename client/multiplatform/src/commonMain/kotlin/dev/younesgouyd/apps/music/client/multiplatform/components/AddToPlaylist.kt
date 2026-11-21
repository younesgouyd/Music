package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.components.util.Image
import dev.younesgouyd.apps.music.client.multiplatform.components.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.client.multiplatform.components.util.PageSize
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.client.multiplatform.usecases.AddToPlaylistUseCase
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AddToPlaylist(
    tracks: Set<TrackId> = emptySet(),
    playlists: Set<PlaylistId> = emptySet(),
    folders: Set<FolderId> = emptySet(),
    artists: Set<SpotifyArtistId> = emptySet(),
    albums: Set<SpotifyAlbumId> = emptySet(),
    playlistRepo: PlaylistRepo,
    addToPlaylistUseCase: AddToPlaylistUseCase,
    onDone: AddToPlaylist.() -> Unit
) : Component() {
    override val title: String = "Add to Playlist"
    private val state: Ui.State
    private val _adding: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val adding = _adding.asStateFlow()

    init {
        if (tracks.isEmpty() && playlists.isEmpty() && folders.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
            TODO()
        }
        state = Ui.State(
            adding = _adding.asStateFlow(),
            playlists = LazilyLoadedItems(
                scope = coroutineScope,
                loader = { offset: Offset.Id<PlaylistId>, pageSize: PageSize ->
                    val rows = playlistRepo.getAll(pageSize, offset)
                    LazilyLoadedItems.Page(
                        nextOffset = rows.lastOrNull()?.id?.let { Offset.Id(it) },
                        items = rows.map { Ui.State.PlaylistOption(it.id, it.name, null) }
                    )
                },
                initialOffset = Offset.Id.initial()
            ),
            onDoneClick = { playlistToAddTo: PlaylistToAddTo ->
                coroutineScope.launch {
                    _adding.update { true }
                    addToPlaylistUseCase.execute(
                        tracks = tracks,
                        playlists = playlists,
                        folders = folders,
                        artists = artists,
                        albums = albums,
                        playlist = playlistToAddTo
                    )
                    _adding.update { false }
                    onDone()
                }
            }
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(
            modifier = modifier.size(width = 500.dp, height = 600.dp),
            state = state
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        class State(
            val adding: StateFlow<Boolean>,
            val playlists: LazilyLoadedItems<PlaylistOption, Offset.Id<PlaylistId>>,
            val onDoneClick: (playlistId: PlaylistToAddTo) -> Unit
        ) {
            data class PlaylistOption(
                val id: PlaylistId,
                val name: String,
                val image: MediaFileId?
            )
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            val adding by state.adding.collectAsState()
            val playlists = state.playlists
            val items by playlists.items.collectAsState()
            val loadingItems by playlists.loading.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                if (adding) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Please wait...")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            text = "Add to playlist",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            state = lazyColumnState,
                            contentPadding = PaddingValues(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                var name by rememberSaveable { mutableStateOf("") }

                                Item(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.Image(
                                            modifier = Modifier.size(64.dp),
                                            imageVector = Icons.AutoMirrored.Default.PlaylistAdd,
                                            contentDescription = null
                                        )
                                        OutlinedTextField(
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Create new playlist") },
                                            value = name,
                                            onValueChange = { name = it }
                                        )
                                        Button(
                                            content = { Text("Done") },
                                            onClick = {
                                                state.onDoneClick(
                                                    PlaylistToAddTo.New(name)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            items(
                                items = items,
                                key = { it.id.value }
                            ) { playlistOption ->
                                Item(
                                    modifier = Modifier.padding(8.dp),
                                    onClick = {
                                        state.onDoneClick(
                                            PlaylistToAddTo.Id(playlistOption.id)
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            modifier = Modifier.size(64.dp),
                                            id = playlistOption.image
                                        )
                                        Text(
                                            text = playlistOption.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (loadingItems) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(lazyColumnState, playlists) {
                snapshotFlow {
                    lazyColumnState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                }.map { it == null ||  it >= (items.size + 1) - 5  }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { playlists.loadMore() }
            }
        }
    }
}