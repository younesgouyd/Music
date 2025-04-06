package dev.younesgouyd.apps.music.desktop.components

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
import dev.younesgouyd.apps.music.common.components.Queue
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.desktop.components.util.widgets.Image

class Queue(
    mediaController: MediaController
) : Queue(mediaController) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
                color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        text = if (item.artists.isEmpty()) item.name else "${item.artists.first().name} - ${item.name}",
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
            var isExpanded by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) {
                    if (isExpanded) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
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
                            when (isExpanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                    if (isExpanded) {
                        item.items.forEachIndexed { index, track ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp),
                                enabled = enabled,
                                shape = MaterialTheme.shapes.large,
                                color = if (isPlaying) {
                                    if (playingItem == index) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
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
                                        text = if (track.artists.isEmpty()) track.name else "${track.artists.first().name} - ${track.name}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                        Box(Modifier)
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
            var isExpanded by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) {
                    if (isExpanded) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
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
                            when (isExpanded) {
                                true -> Icon(Icons.Default.ArrowDropUp, null)
                                false -> Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                    if (isExpanded) {
                        item.items.forEachIndexed { index, track ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp),
                                enabled = enabled,
                                shape = MaterialTheme.shapes.large,
                                color = if (isPlaying) {
                                    if (playingItem == index) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
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
                                        text = if (track.artists.isEmpty()) track.name else "${track.artists.first().name} - ${track.name}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                        Box(Modifier)
                    }
                }
            }
        }
    }
}
