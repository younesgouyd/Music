package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaControllerGeneralState
import dev.younesgouyd.apps.music.common.models.RepeatState
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Client
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MediaController(
    private val server: Server
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}

    // UI STATE
    private val clients: StateFlow<List<Client>>
    private val selectedClient: StateFlow<ClientId?>
    private val enabled: StateFlow<Boolean>
    private val queue: StateFlow<List<QueueItem>>
    private val currentItemIndex: StateFlow<Int>
    private val repeatState: StateFlow<RepeatState>
    private val mediaPlayerState: StateFlow<MediaPlayerState?>
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)

    val state: StateFlow<MediaControllerState> get() = _state.asStateFlow()

    init {
        clients = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.Clients)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
        selectedClient = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.SelectedClient)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        enabled = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.Enabled)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
        queue = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.Queue)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
        currentItemIndex = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.QueueItemIndex)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)
        repeatState = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.RepeatState)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), RepeatState.Off)
        mediaPlayerState = server.observe(Rpc.ToServer.MediaController.ObserveAvailableState.MediaPlayerState)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        val currentItem = combine(queue, currentItemIndex, ::Pair)
            .mapLatest { (queue, index) -> if (queue.isNotEmpty()) queue[index] else null }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        coroutineScope.launch {
            val selectedClient = selectedClient.filterNotNull().stateIn(coroutineScope)
            val positionMs = mediaPlayerState.combine(currentItem.map { it?.durationMs }, ::Pair)
                .transformLatest { (state: MediaPlayerState?, durationMs: Long?) ->
                    if (state == null || durationMs == null) {
                        emit(null)
                    } else if (state.isPlaying) {
                        while (true) {
                            val elapsedTimeMs: Long = state.positionMs
                            emit(elapsedTimeMs.coerceAtMost(durationMs))
                            if (elapsedTimeMs >= durationMs) break
                            delay(50.milliseconds)
                        }
                    } else {
                        emit(state.positionMs.coerceAtMost(durationMs))
                    }
                }.stateIn(coroutineScope, SharingStarted.Eagerly, null)
            server.observe(Rpc.ToServer.MediaController.ObserveState).collect { state ->
                _state.value = when (state) {
                    MediaControllerGeneralState.Loading -> MediaControllerState.Loading
                    MediaControllerGeneralState.Unavailable -> MediaControllerState.Unavailable
                    MediaControllerGeneralState.Available -> MediaControllerState.Available(
                        clients = clients,
                        selectedClient = selectedClient,
                        enabled = enabled,
                        queue = queue,
                        queueItemIndex = currentItemIndex,
                        currentItem = currentItem,
                        repeatState = repeatState,
                        positionMs = positionMs,
                        mediaPlayerState = mediaPlayerState
                    )
                }
            }
        }
    }

    fun selectClient(id: ClientId) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.SelectClient(id))
            }
        }
    }

    fun playQueue(queue: List<Rpc.ToServer.MediaController.QueueItemParameter>, queueItemIndex: Int = 0) {
        if (queue.isEmpty()) {
            logger.warn { "::playQueue | queue is empty" }
            return
        }
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.PlayQueue(queue, queueItemIndex))
            }
        }
    }

    fun play() {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Play)
            }
        }
    }

    fun pause() {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Pause)
            }
        }
    }

    fun seek(positionMs: Long) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Seek(positionMs))
            }
        }
    }

    fun next() {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Next)
            }
        }
    }

    fun previous() {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Previous)
            }
        }
    }

    fun repeat() {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.Repeat)
            }
        }
    }

    fun addToQueue(items: List<Rpc.ToServer.MediaController.QueueItemParameter>) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.AddToQueue(items))
            }
        }
    }

    fun playItem(index: Int) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.PlayItem(index))
            }
        }
    }

    fun changeItemIndex(oldIndex: Int, newIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.ChangeItemIndex(oldIndex, newIndex))
            }
        }
    }

    fun removeItem(key: Int) {
        coroutineScope.launch {
            mutex.withLock {
                server.call(Rpc.ToServer.MediaController.RemoveItem(key))
            }
        }
    }

    fun release() {
        coroutineScope.cancel()
    }
}