package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.components.util.MediaController
import dev.younesgouyd.apps.music.app.components.util.widgets.Image
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
                is MediaController.MediaControllerState.Available -> Main(
                    modifier = modifier,
                    mediaControllerState = state
                )
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            mediaControllerState: MediaController.MediaControllerState.Available
        ) {
            val enabled by mediaControllerState.enabled.collectAsState()

            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        itemsIndexed(
                            items = mediaControllerState.playbackState.queue
                        ) { index: Int, queueItem: MediaController.MediaControllerState.Available.PlaybackState.QueueItem ->
                            when (queueItem) {
                                is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Track -> {
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        isPlaying = mediaControllerState.playbackState.queueItemIndex == index,
                                        enabled = enabled,
                                        onClick = { mediaControllerState.onPlayQueueItem(index) }
                                    )
                                }

                                is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> {
                                    PlaylistItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        isPlaying = mediaControllerState.playbackState.queueItemIndex == index,
                                        playingItem = if (mediaControllerState.playbackState.queueItemIndex == index) { mediaControllerState.playbackState.queueSubItemIndex } else { null },
                                        onTrackClick = { trackIndex ->
                                            mediaControllerState.onPlayQueueSubItem(
                                                index,
                                                trackIndex
                                            )
                                        }
                                    )
                                }

                                is MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Album -> {
                                    AlbumItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        isPlaying = mediaControllerState.playbackState.queueItemIndex == index,
                                        playingItem = if (mediaControllerState.playbackState.queueItemIndex == index) { mediaControllerState.playbackState.queueSubItemIndex } else { null },
                                        onTrackClick = { trackIndex ->
                                            mediaControllerState.onPlayQueueSubItem(
                                                index,
                                                trackIndex
                                            )
                                        }
                                    )
                                }
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
            isPlaying: Boolean,
            enabled: Boolean,
            onClick: () -> Unit
        ) {
            Surface(
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier)
                    Icon(Icons.Default.Audiotrack, null)
                    Image(
                        modifier = Modifier.size(80.dp),
                        data = item.album?.image
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = if (item.artists.isEmpty()) item.name else "${item.name} - ${item.artists.first().name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        @Composable
        private fun AlbumItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Album,
            isPlaying: Boolean,
            playingItem: Int?,
            enabled: Boolean,
            onTrackClick: (index: Int) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier)
                        Icon(Icons.Default.Album, null)
                        Image(
                            modifier = Modifier.size(80.dp),
                            data = item.image
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            when (expanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                    if (expanded) {
                        item.items.forEachIndexed { index, item ->
                            Box(
                                modifier = Modifier
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                    enabled = enabled,
                                    shape = MaterialTheme.shapes.large,
                                    color = if (isPlaying && playingItem == index) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                    onClick = { onTrackClick(index) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier)
                                        Icon(Icons.Default.Audiotrack, null)
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = if (item.artists.isEmpty()) item.name else "${item.name} - ${item.artists.first().name}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun PlaylistItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.PlaybackState.QueueItem.Playlist,
            isPlaying: Boolean,
            playingItem: Int?,
            enabled: Boolean,
            onTrackClick: (index: Int) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier)
                        Icon(Icons.AutoMirrored.Default.QueueMusic, null)
                        Image(
                            modifier = Modifier.size(80.dp),
                            data = item.image
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            when (expanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                    if (expanded) {
                        item.items.forEachIndexed { index, track ->
                            TrackItem(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                item = track,
                                isPlaying = isPlaying && playingItem == index,
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
