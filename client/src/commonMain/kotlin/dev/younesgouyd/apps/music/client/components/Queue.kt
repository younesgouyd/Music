package dev.younesgouyd.apps.music.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        val listState = LazyListState()
        val enableAutoScrollingToCurrentItem = MutableStateFlow(true)
        state = mediaController.state.mapLatest { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> QueueState.Unavailable
                is MediaController.MediaControllerState.Loading -> QueueState.Loading
                is MediaController.MediaControllerState.Available -> {
                    QueueState.Available(
                        enabled = mediaControllerState.enabled,
                        queue = mediaControllerState.queue,
                        currentItem = mediaControllerState.currentItem,
                        currentItemIndex = mediaControllerState.queueItemIndex,
                        listState = listState,
                        enableAutoScrollingToCurrentItem = enableAutoScrollingToCurrentItem.asStateFlow(),
                        onEnableAutoScrollingToCurrentItemChange = { enableAutoScrollingToCurrentItem.value = it },
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

        Ui.Main(modifier = modifier, state = state)
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
            val currentItemIndex: StateFlow<Int>,
            val listState: LazyListState,
            val enableAutoScrollingToCurrentItem: StateFlow<Boolean>,
            val onEnableAutoScrollingToCurrentItemChange: (Boolean) -> Unit,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val changeItemIndex: (from: Int, to: Int) -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onRemoveFromQueueClick: (key: Int) -> Unit,
            val onCloseClick: () -> Unit
        ) : QueueState()
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
            val queue by state.queue.collectAsState()
            val currentItem by state.currentItem.collectAsState()
            val currentItemIndex by state.currentItemIndex.collectAsState()
            val enableAutoScrollingToCurrentItem by state.enableAutoScrollingToCurrentItem.collectAsState()
            var orderedItems by remember { mutableStateOf(queue) }
            var programmaticScrolling by remember { mutableStateOf(false) }
            var isDragging by remember { mutableStateOf(false) }
            val disableAutoScrollingToPlayingItem by remember {
                derivedStateOf { isDragging || (state.listState.isScrollInProgress && !programmaticScrolling) }
            }
            var dragged by remember { mutableStateOf(false) }
            val reorderState = rememberReorderableLazyListState(
                onMove = { fromItem, toItem ->
                    isDragging = true
                    orderedItems = orderedItems.toMutableList().apply {
                        add(toItem.index, removeAt(fromItem.index))
                    }
                },
                listState = state.listState,
                canDragOver = { _, _ -> enabled },
                onDragEnd = { from, to ->
                    isDragging = false
                    dragged = true
                    state.changeItemIndex(from, to)
                }
            )
            val scope = rememberCoroutineScope()

            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 8.dp),
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
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f)
                            .reorderable(reorderState)
                            .then(
                                when (platform) {
                                    Platform.ANDROID -> Modifier.detectReorderAfterLongPress(reorderState)
                                    Platform.JVM -> Modifier.detectReorder(reorderState)
                                }
                            ),
                        state = state.listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
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
                    Surface(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch { state.listState.animateScrollToItem(0) }
                                },
                                content = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                            )
                            IconButton(
                                onClick = {
                                    scope.launch { state.listState.animateScrollToItem(state.listState.layoutInfo.totalItemsCount-1) }
                                },
                                content = { Icon(Icons.Default.KeyboardDoubleArrowDown, null) }
                            )
                            FilledIconToggleButton(
                                checked = enableAutoScrollingToCurrentItem,
                                onCheckedChange = { state.onEnableAutoScrollingToCurrentItemChange(it) }
                            ) {
                                Icon(
                                    imageVector = if (enableAutoScrollingToCurrentItem) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    AdaptiveUi(
                        wide = {},
                        compact = {
                            IconButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = state.onCloseClick,
                                content = { Icon(Icons.Default.Close, null) }
                            )
                        }
                    )
                }
            }

            LaunchedEffect(queue) {
                if (!isDragging) {
                    orderedItems = queue
                }
            }

            LaunchedEffect(enableAutoScrollingToCurrentItem, currentItemIndex, isDragging, orderedItems) {
                if (enableAutoScrollingToCurrentItem && !isDragging && !dragged && orderedItems.isNotEmpty()) {
                    val index = currentItemIndex
                    if (index in orderedItems.indices) {
                        programmaticScrolling = true
                        state.listState.animateScrollToItem(index)
                        programmaticScrolling = false
                    }
                }
                dragged = false
            }

            LaunchedEffect(disableAutoScrollingToPlayingItem) {
                if (disableAutoScrollingToPlayingItem) {
                    state.onEnableAutoScrollingToCurrentItemChange(false)
                }
            }
        }

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
    }
}