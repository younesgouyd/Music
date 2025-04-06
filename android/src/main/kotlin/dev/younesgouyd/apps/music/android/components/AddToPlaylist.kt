package dev.younesgouyd.apps.music.android.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.android.components.util.widgets.Image
import dev.younesgouyd.apps.music.android.components.util.widgets.Item
import dev.younesgouyd.apps.music.common.components.AddToPlaylist
import dev.younesgouyd.apps.music.common.data.repoes.*

class AddToPlaylist(
    itemToAdd: Item,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    trackRepo: TrackRepo,
    albumRepo: AlbumRepo,
    folderRepo: FolderRepo,
    dismiss: () -> Unit,
    playlistRepo: PlaylistRepo
) : AddToPlaylist(
    itemToAdd, playlistTrackCrossRefRepo, trackRepo, albumRepo, folderRepo, dismiss, playlistRepo
) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(
            modifier = modifier,
            state = state
        )
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: AddToPlaylistState
        ) {
            when (state) {
                is AddToPlaylistState.Loading -> Text(modifier = modifier, text = "Loading...")
                is AddToPlaylistState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }
        @Composable
        fun Main(
            modifier: Modifier,
            loaded: AddToPlaylistState.Loaded
        ) {
            val adding by loaded.adding.collectAsState()
            val itemToAdd = loaded.itemToAdd
            val playlists by loaded.playlists.collectAsState()
            val lazyColumnState = rememberLazyListState()

            if (!adding) {
                Surface(
                    modifier = modifier.size(width = 500.dp, height = 600.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(64.dp),
                                data = itemToAdd.image
                            )
                            Text(
                                text = itemToAdd.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                state = lazyColumnState,
                                contentPadding = PaddingValues(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    var name by remember { mutableStateOf("") }

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
                                                onClick = { loaded.onAddTopPlaylist(AddToPlaylistState.Loaded.PlaylistToAddTo.New(name)) }
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = playlists,
                                    key = { it.id }
                                ) { playlistOption ->
                                    Item(
                                        modifier = Modifier.padding(8.dp),
                                        onClick = { loaded.onAddTopPlaylist(AddToPlaylistState.Loaded.PlaylistToAddTo.Id(playlistOption.id)) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                modifier = Modifier.size(64.dp),
                                                data = playlistOption.image
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
                            }
                        }
                    }
                }
            } else {
                Text(modifier = modifier, text = "Adding...")
            }
        }
    }
}