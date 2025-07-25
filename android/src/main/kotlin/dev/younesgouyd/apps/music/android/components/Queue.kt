package dev.younesgouyd.apps.music.android.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.android.components.util.widgets.Image
import dev.younesgouyd.apps.music.common.components.Queue
import dev.younesgouyd.apps.music.common.components.util.MediaController

class Queue(
    mediaController: MediaController,
    close: () -> Unit
) : Queue(mediaController, close) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier = Modifier, state: QueueState) {
            when (state) {
                is QueueState.Loading -> Unit
                is QueueState.Unavailable -> Unit
                is QueueState.Available -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            state: QueueState.Available
        ) {
            val enabled by state.enabled.collectAsState()
            val queue = state.queue

            Surface(
                modifier = modifier,
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
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        itemsIndexed(
                            items = queue
                        ) { index: Int, queueItem: MediaController.MediaControllerState.Available.QueueItem ->
                            when (queueItem) {
                                is MediaController.MediaControllerState.Available.QueueItem.Track -> {
                                    TrackItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        isPlaying = state.queueItemIndex == index,
                                        enabled = enabled,
                                        onClick = { state.onPlayQueueItem(index) }
                                    )
                                }

                                is MediaController.MediaControllerState.Available.QueueItem.Playlist -> {
                                    PlaylistItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        isPlaying = state.queueItemIndex == index,
                                        playingItem = if (state.queueItemIndex == index) { state.queueSubItemIndex } else { null },
                                        onTrackClick = { trackIndex ->
                                            state.onPlayQueueSubItem(
                                                index,
                                                trackIndex
                                            )
                                        }
                                    )
                                }

                                is MediaController.MediaControllerState.Available.QueueItem.Album -> {
                                    AlbumItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        enabled = enabled,
                                        isPlaying = state.queueItemIndex == index,
                                        playingItem = if (state.queueItemIndex == index) { state.queueSubItemIndex } else { null },
                                        onTrackClick = { trackIndex ->
                                            state.onPlayQueueSubItem(
                                                index,
                                                trackIndex
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = state.onCloseClick,
                        content = { Icon(Icons.Default.Close, null) }
                    )
                }
            }
        }

        @Composable
        private fun TrackItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.QueueItem.Track,
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
            item: MediaController.MediaControllerState.Available.QueueItem.Album,
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
            item: MediaController.MediaControllerState.Available.QueueItem.Playlist,
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
