package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
import dev.younesgouyd.apps.music.app.components.util.widgets.MediaController
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

class Queue(
    private val mediaController: MediaController
) : Component() {
    override val title: String = "Queue"
    private val state: StateFlow<MediaController.MediaControllerState>
        get() = mediaController.state

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier = Modifier, state: MediaController.MediaControllerState) {
            when (state) {
                is MediaController.MediaControllerState.Loading -> Unit
                is MediaController.MediaControllerState.Unavailable -> Unit
                is MediaController.MediaControllerState.Available -> Main(modifier = modifier, mediaControllerState = state)
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            mediaControllerState: MediaController.MediaControllerState.Available
        ) {
            val enabled by mediaControllerState.enabled.collectAsState()

            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(),
                colors = CardDefaults.elevatedCardColors(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    stickyHeader {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Default.QueueMusic, null)
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    itemsIndexed(
                        items = mediaControllerState.playbackState.queue
                    ) { index: Int, queueItem: MediaController.MediaControllerState.Available.PlaybackState.QueueItem ->
                        when (queueItem) {
                            is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Track -> {
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    item = queueItem,
                                    enabled = enabled,
                                    onClick = { mediaControllerState.onPlayQueueItem(index) }
                                )
                            }
                            is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> {
                                PlaylistItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    item = queueItem,
                                    enabled = enabled,
                                    onTrackClick = { trackIndex -> mediaControllerState.onPlayQueueSubItem(index, trackIndex) }
                                )
                            }
                            is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Album -> {
                                AlbumItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    item = queueItem,
                                    enabled = enabled,
                                    onTrackClick = { trackIndex -> mediaControllerState.onPlayQueueSubItem(index, trackIndex) }
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Track,
            enabled: Boolean,
            onClick: () -> Unit
        ) {
            Card(
                modifier = modifier,
                enabled = enabled,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Audiotrack, null)
                    Image(
                        modifier = Modifier.size(80.dp),
                        url = null // todo
                    )
                    Text(
                        text = if (item.artists.isEmpty()) item.name else "${item.name} - ${item.artists.first().name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        @Composable
        private fun PlaylistItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Playlist,
            enabled: Boolean,
            onTrackClick: (index: Int) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = modifier,
                onClick = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Default.QueueMusic, null)
                    Image(
                        modifier = Modifier.size(80.dp),
                        url = null // todo
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        when (expanded) {
                            true -> Icon(Icons.Default.ArrowDropUp, null)
                            false -> Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    if (expanded) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = item.items
                            ) { index: Int, item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    item = item,
                                    enabled = enabled,
                                    onClick = { onTrackClick(index) }
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun AlbumItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Album,
            enabled: Boolean,
            onTrackClick: (index: Int) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                onClick = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Album, null)
                    Image(
                        modifier = Modifier.size(80.dp),
                        url = null // todo
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        when (expanded) {
                            true -> Icon(Icons.Default.ArrowDropUp, null)
                            false -> Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    if (expanded) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = item.items
                            ) { index: Int, item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Track ->
                                TrackItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    item = item,
                                    enabled = enabled,
                                    onClick = { onTrackClick(index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
