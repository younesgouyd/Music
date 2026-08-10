package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.MediaController
import dev.younesgouyd.apps.music.client.common.components.util.Artists
import dev.younesgouyd.apps.music.client.common.components.util.Image
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.Platform
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.platform
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

class Queue(
    mediaController: MediaController,
    showArtist: (SpotifyArtistId) -> Unit
) : Component() {
    override val title: String = "Queue"
    private val state: StateFlow<Ui.State>

    init {
        val listState = LazyListState()
        val enableAutoScrollingToCurrentItem = MutableStateFlow(true)
        state = mediaController.state.map { mediaControllerState ->
            when (mediaControllerState) {
                is MediaController.MediaControllerState.Unavailable -> Ui.State.Unavailable
                is MediaController.MediaControllerState.Loading -> Ui.State.Loading
                is MediaController.MediaControllerState.Available -> {
                    Ui.State.Available(
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
                        onRemoveFromQueueClick = mediaController::removeItem
                    )
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Unavailable)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }


    private object Ui {
        sealed class State {
            data object Loading : State()

            data object Unavailable : State()

            data class Available(
                val enabled: StateFlow<Boolean>,
                val queue: StateFlow<List<MediaController.MediaControllerState.Available.QueueItem>>,
                val currentItem: StateFlow<MediaController.MediaControllerState.Available.QueueItem?>,
                val currentItemIndex: StateFlow<Int>,
                val listState: LazyListState,
                val enableAutoScrollingToCurrentItem: StateFlow<Boolean>,
                val onEnableAutoScrollingToCurrentItemChange: (Boolean) -> Unit,
                val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
                val changeItemIndex: (from: Int, to: Int) -> Unit,
                val onArtistClick: (SpotifyArtistId) -> Unit,
                val onRemoveFromQueueClick: (key: Int) -> Unit
            ) : State()
        }

        @Composable
        fun Main(modifier: Modifier = Modifier, state: State) {
            when (state) {
                is State.Loading -> Unit
                is State.Unavailable -> Unit
                is State.Available -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            state: State.Available
        ) {
            Main(
                modifier = modifier,
                enabled = state.enabled,
                queue = state.queue,
                currentItem = state.currentItem,
                currentItemIndex = state.currentItemIndex,
                listState = state.listState,
                enableAutoScrollingToCurrentItem = state.enableAutoScrollingToCurrentItem,
                onEnableAutoScrollingToCurrentItemChange = state.onEnableAutoScrollingToCurrentItemChange,
                onPlayQueueItem = state.onPlayQueueItem,
                changeItemIndex = state.changeItemIndex,
                onArtistClick = state.onArtistClick,
                onRemoveFromQueueClick = state.onRemoveFromQueueClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier = Modifier,
            enabled: StateFlow<Boolean>,
            queue: StateFlow<List<MediaController.MediaControllerState.Available.QueueItem>>,
            currentItem: StateFlow<MediaController.MediaControllerState.Available.QueueItem?>,
            currentItemIndex: StateFlow<Int>,
            listState: LazyListState,
            enableAutoScrollingToCurrentItem: StateFlow<Boolean>,
            onEnableAutoScrollingToCurrentItemChange: (Boolean) -> Unit,
            onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            changeItemIndex: (from: Int, to: Int) -> Unit,
            onArtistClick: (SpotifyArtistId) -> Unit,
            onRemoveFromQueueClick: (key: Int) -> Unit
        ) {
            val enabled by enabled.collectAsState()
            val queue by queue.collectAsState()
            val currentItem by currentItem.collectAsState()
            val currentItemIndex by currentItemIndex.collectAsState()
            val enableAutoScrollingToCurrentItem by enableAutoScrollingToCurrentItem.collectAsState()
            var orderedItems by remember { mutableStateOf(queue) }
            var programmaticScrolling by remember { mutableStateOf(false) }
            var isDragging by remember { mutableStateOf(false) }
            val disableAutoScrollingToPlayingItem by remember {
                derivedStateOf { isDragging || (listState.isScrollInProgress && !programmaticScrolling) }
            }

            var dragged by remember { mutableStateOf(false) }
            val reorderState = rememberReorderableLazyListState(
                onMove = { fromItem, toItem ->
                    isDragging = true
                    orderedItems = orderedItems.toMutableList().apply {
                        add(toItem.index, removeAt(fromItem.index))
                    }
                },
                listState = listState,
                canDragOver = { _, _ -> enabled },
                onDragEnd = { from, to ->
                    isDragging = false
                    dragged = true
                    changeItemIndex(from, to)
                }
            )
            val scope = rememberCoroutineScope()

            Surface(
                modifier = modifier,
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
                        state = listState,
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
                                    isPlaying = currentItem?.key == queueItem.key,
                                    enabled = enabled,
                                    tonalElevation = elevation,
                                    onClick = { onPlayQueueItem(index) },
                                    onArtistClick = onArtistClick,
                                    onRemoveClick = { onRemoveFromQueueClick(queueItem.key!!) }
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
                                    scope.launch { listState.animateScrollToItem(0) }
                                },
                                content = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(
                                            index = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                        )
                                    }
                                },
                                content = { Icon(Icons.Default.KeyboardDoubleArrowDown, null) }
                            )
                            FilledIconToggleButton(
                                checked = enableAutoScrollingToCurrentItem,
                                onCheckedChange = { onEnableAutoScrollingToCurrentItemChange(it) }
                            ) {
                                Icon(
                                    imageVector = if (enableAutoScrollingToCurrentItem) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null
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

            LaunchedEffect(enableAutoScrollingToCurrentItem, currentItemIndex, isDragging, orderedItems) {
                if (enableAutoScrollingToCurrentItem && !isDragging && !dragged && orderedItems.isNotEmpty()) {
                    val index = currentItemIndex
                    if (index in orderedItems.indices) {
                        programmaticScrolling = true
                        listState.animateScrollToItem(index)
                        programmaticScrolling = false
                    }
                }
                dragged = false
            }

            LaunchedEffect(disableAutoScrollingToPlayingItem) {
                if (disableAutoScrollingToPlayingItem) {
                    onEnableAutoScrollingToCurrentItemChange(false)
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
            onArtistClick: (SpotifyArtistId) -> Unit,
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
                        when (item.artists) {
                            is MediaController.MediaControllerState.Available.QueueItem.Artists.ImportArtist -> {
                                Artists(names = item.artists.list)
                            }
                            is MediaController.MediaControllerState.Available.QueueItem.Artists.SpotifyArtists -> {
                                Artists(artists = item.artists.list, onArtistClick = onArtistClick)
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