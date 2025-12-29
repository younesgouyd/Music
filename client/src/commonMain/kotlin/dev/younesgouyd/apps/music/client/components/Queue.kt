package dev.younesgouyd.apps.music.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.Platform
import dev.younesgouyd.apps.music.client.components.util.AdaptiveUi
import dev.younesgouyd.apps.music.client.components.util.Image
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.platform
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalCoroutinesApi::class)
class Queue(
    mediaController: MediaController,
    showArtist: (ArtistId) -> Unit,
    close: () -> Unit
) : Component() {
    override val title: String = "Queue"
    private val state: StateFlow<QueueState>

    init {
        var scrollState = LazyListState()
        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> QueueState.Unavailable
                is MediaController.MediaControllerState.Loading -> QueueState.Loading
                is MediaController.MediaControllerState.Available -> {
                    QueueState.Available(
                        enabled = mediaControllerState.enabled,
                        queue = mediaControllerState.queue,
                        currentItem = mediaControllerState.currentItem,
                        scrollState = scrollState,
                        onPlayQueueItem = mediaController::playItem,
                        changeItemIndex = mediaController::changeItemIndex,
                        onArtistClick = showArtist,
                        onRemoveFromQueueClick = mediaController::removeItem,
                        onCloseClick = close
                    )
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), QueueState.Unavailable)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        AdaptiveUi(
            wide = { Ui.Wide.Main(modifier = modifier, state = state) },
            compact = { Ui.Compact.Main(modifier = modifier, state = state) }
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class QueueState {
        data object Loading : QueueState()

        data object Unavailable : QueueState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val queue: StateFlow<List<MediaController.MediaControllerState.Available.QueueItem>>,
            val currentItem: StateFlow<MediaController.MediaControllerState.Available.QueueItem>,
            val scrollState: LazyListState,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val changeItemIndex: (from: Int, to: Int) -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onRemoveFromQueueClick: (key: Int) -> Unit,
            val onCloseClick: () -> Unit
        ) : QueueState()
    }

    private object Ui {
        @Composable
        private fun QueueItem(
            modifier: Modifier = Modifier,
            item: MediaController.MediaControllerState.Available.QueueItem,
            isPlaying: Boolean,
            enabled: Boolean,
            tonalElevation: Dp,
            onClick: () -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onRemoveClick: () -> Unit
        ) {
            Surface(
                modifier = modifier.fillMaxWidth().height(100.dp),
                enabled = enabled,
                shape = MaterialTheme.shapes.large,
                color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = tonalElevation,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                        file = item.image
                    )
                    Column(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = item.artists, key = { it.id.value }) { artist ->
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
                                    },
                                    enabled = enabled
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onRemoveClick,
                        content = { Icon(Icons.Default.Remove, null) },
                        enabled = enabled
                    )
                }
            }
        }

        object Wide {
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
                val queue by state.queue.collectAsState()
                val currentItem by state.currentItem.collectAsState()
                var orderedItems by remember { mutableStateOf(queue) }
                var isDragging by remember { mutableStateOf(false) }
                val reorderState = rememberReorderableLazyListState(
                    onMove = { fromItem, toItem ->
                        isDragging = true
                        orderedItems = orderedItems.toMutableList().apply {
                            add(toItem.index, removeAt(fromItem.index))
                        }
                    },
                    listState = state.scrollState,
                    canDragOver = { _, _ -> enabled },
                    onDragEnd = { from, to ->
                        isDragging = false
                        state.changeItemIndex(from, to)
                    }
                )

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
                            modifier = Modifier.fillMaxSize()
                                .reorderable(reorderState)
                                .then(
                                    when (platform) {
                                        Platform.ANDROID -> Modifier.detectReorderAfterLongPress(reorderState)
                                        Platform.JVM -> Modifier.detectReorder(reorderState)
                                    }
                                ),
                            state = state.scrollState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            itemsIndexed(
                                items = orderedItems,
                                key = { _, item -> item.key.toString() }
                            ) { index: Int, queueItem: MediaController.MediaControllerState.Available.QueueItem ->
                                ReorderableItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    state = reorderState,
                                    key = queueItem.key.toString(),
                                    defaultDraggingModifier = Modifier.animateItem()
                                ) { isDragging ->
                                    val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                    QueueItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        item = queueItem,
                                        isPlaying = currentItem.key == queueItem.key,
                                        enabled = enabled,
                                        tonalElevation = elevation,
                                        onClick = { state.onPlayQueueItem(index) },
                                        onArtistClick = state.onArtistClick,
                                        onRemoveClick = { state.onRemoveFromQueueClick(queueItem.key!!) }
                                    )
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(queue) {
                    if (!isDragging) {
                        orderedItems = queue
                    }
                }
            }
        }

        object Compact {
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
                val queue by state.queue.collectAsState()
                val currentItem by state.currentItem.collectAsState()
                var orderedItems by remember { mutableStateOf(queue) }
                var isDragging by remember { mutableStateOf(false) }
                val reorderState = rememberReorderableLazyListState(
                    onMove = { fromItem, toItem ->
                        isDragging = true
                        orderedItems = orderedItems.toMutableList().apply {
                            add(toItem.index, removeAt(fromItem.index))
                        }
                    },
                    listState = state.scrollState,
                    canDragOver = { _, _ -> enabled },
                    onDragEnd = { from, to ->
                        isDragging = false
                        state.changeItemIndex(from, to)
                    }
                )

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
                            modifier = Modifier.fillMaxSize()
                                .weight(1f)
                                .reorderable(reorderState)
                                .then(
                                    when (platform) {
                                        Platform.ANDROID -> Modifier.detectReorderAfterLongPress(reorderState)
                                        Platform.JVM -> Modifier.detectReorder(reorderState)
                                    }
                                ),
                            state = state.scrollState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            itemsIndexed(
                                items = orderedItems,
                                key = { _, item -> item.key.toString() }
                            ) { index: Int, queueItem: MediaController.MediaControllerState.Available.QueueItem ->
                                ReorderableItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    state = reorderState,
                                    key = queueItem.key.toString(),
                                    defaultDraggingModifier = Modifier.animateItem()
                                ) { isDragging ->
                                    val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                    QueueItem(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        item = queueItem,
                                        isPlaying = currentItem.key == queueItem.key,
                                        enabled = enabled,
                                        tonalElevation = elevation,
                                        onClick = { state.onPlayQueueItem(index) },
                                        onArtistClick = state.onArtistClick,
                                        onRemoveClick = { state.onRemoveFromQueueClick(queueItem.key!!) }
                                    )
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

                LaunchedEffect(queue) {
                    if (!isDragging) {
                        orderedItems = queue
                    }
                }
            }
        }
    }
}