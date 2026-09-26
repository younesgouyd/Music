package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.playback.*
import dev.younesgouyd.apps.music.common.models.rpc.MediaPlayerRpc
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.SpotifyAlbumRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.TrackRelation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.uuid.Uuid
import dev.younesgouyd.apps.music.common.models.playback.Client as ClientModel

class MediaController(
    private val mediaFileRepo: MediaFileRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: SpotifyArtistRepo,
    private val albumRepo: SpotifyAlbumRepo,
    private val clientManager: ClientManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}

    private val _selectedClient: MutableStateFlow<Uuid?> = MutableStateFlow(null)
    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _queue: MutableStateFlow<List<QueueItem>> = MutableStateFlow(emptyList())
    private val _currentItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val _repeatState: MutableStateFlow<RepeatState> = MutableStateFlow(RepeatState.Off)

    private val _state: MutableStateFlow<MediaControllerGeneralState> = MutableStateFlow(MediaControllerGeneralState.Unavailable)

    private val mediaPlayer: MediaPlayerImpl = MediaPlayerImpl()

    val clients: StateFlow<List<ClientModel>> = clientManager.clients
        .map { it.map { ClientModel(it.id) } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val selectedClient: StateFlow<Uuid?> = _selectedClient.asStateFlow()
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()
    val currentItemIndex: StateFlow<Int> = _currentItemIndex.asStateFlow()
    val mediaPlayerState: StateFlow<MediaPlayerState> = mediaPlayer.state
    val repeatState: StateFlow<RepeatState> = _repeatState.asStateFlow()

    val state: StateFlow<MediaControllerGeneralState> get() = _state.asStateFlow()

    init {
        coroutineScope.launch {
            clients.collect { clients ->
                if (clients.isEmpty() || clients.none { it.id == _selectedClient.value }) {
                    selectClient(null)
                }
            }
        }
        coroutineScope.launch {
            mediaPlayerState.filter { it.isFinished }.collect { mediaPlayerState ->
                mutex.withLock {
                    _enabled.value = false
                    when (_repeatState.value) {
                        RepeatState.Off -> {
                            if (_queue.value.isEmpty()) { TODO() }
                            if (mediaPlayerState.isPlaying) { mediaPlayer.stop() }
                            val queue = _queue.value
                            val currentIndex = _currentItemIndex.value
                            if (currentIndex < queue.lastIndex) {
                                val newIndex = currentIndex + 1
                                _currentItemIndex.value = newIndex
                                val newTrack = queue[newIndex]
                                newTrack.audioId?.let {
                                    mediaPlayer.setTrack(it)
                                    mediaPlayer.play()
                                }
                            }
                        }
                        RepeatState.List -> {
                            _next(_selectedClient.value)
                        }
                        RepeatState.Track -> {
                            if (_queue.value.isEmpty()) { TODO() }
                            if (mediaPlayerState.isPlaying) { mediaPlayer.stop() }
                            val queue = _queue.value
                            val track = queue[_currentItemIndex.value]
                            track.audioId?.let {
                                mediaPlayer.setTrack(it)
                                mediaPlayer.play()
                            }
                        }
                    }
                    _enabled.value = true
                }
            }
        }
    }

    fun selectClient(id: Uuid?) {
        launchCommand {
            selectClientImpl(id)
        }
    }

    fun playQueue(clientId: Uuid, queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
        if (queue.isEmpty()) {
            logger.warn { "::playQueue | queue is empty" }
            return
        }
        launchCommand {
            _state.update { currentState ->
                when (currentState) {
                    MediaControllerGeneralState.Unavailable -> MediaControllerGeneralState.Loading
                    MediaControllerGeneralState.Loading -> TODO()
                    MediaControllerGeneralState.Available -> {
                        if (mediaPlayerState.value.isPlaying) {
                            mediaPlayer.pause()
                        }
                        currentState
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    MediaControllerGeneralState.Unavailable -> TODO()
                    MediaControllerGeneralState.Loading, MediaControllerGeneralState.Available -> {
                        playQueueImpl(clientId, queue, queueItemIndex)
                        MediaControllerGeneralState.Available
                    }
                }
            }
        }
    }

    fun play(clientId: Uuid) {
        launchCommand {
            if (!mediaPlayerState.value.isPlaying) {
                if (_selectedClient.value == null) {
                    selectClientImpl(clientId)
                }
                mediaPlayer.play()
            }
        }
    }

    fun pause() {
        launchCommand {
            if (mediaPlayerState.value.isPlaying) {
                mediaPlayer.pause()
            }
        }
    }

    fun seek(position: Duration) {
        launchCommand {
            mediaPlayer.setTime(position)
        }
    }

    fun next(clientId: Uuid) {
        launchCommand {
            _next(clientId)
        }
    }

    fun previous(clientId: Uuid) {
        launchCommand {
            var wasPlaying: Boolean? = null
            if (mediaPlayerState.value.isPlaying) {
                wasPlaying = true
                mediaPlayer.stop()
            } else {
                wasPlaying = false
            }
            val queue = _queue.value
            val currentIndex = _currentItemIndex.value
            val newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
            val newTrack = queue[newIndex]
            newTrack.audioId?.let {
                if (_selectedClient.value == null) {
                    selectClientImpl(clientId)
                }
                mediaPlayer.setTrack(it)
                if (wasPlaying) {
                    mediaPlayer.play()
                }
            }
            _currentItemIndex.value = newIndex
        }
    }

    fun repeat() {
        launchCommand {
            _repeatState.update {
                when (it) {
                    RepeatState.Off -> RepeatState.List
                    RepeatState.List -> RepeatState.Track
                    RepeatState.Track -> RepeatState.Off
                }
            }
        }
    }

    fun addToQueue(clientId: Uuid, items: List<QueueItemParameter>) {
        launchCommand {
            if (_state.value == MediaControllerGeneralState.Unavailable) {
                _state.value = MediaControllerGeneralState.Loading
            }
            _state.update { currentState ->
                when (currentState) {
                    MediaControllerGeneralState.Unavailable -> TODO()
                    MediaControllerGeneralState.Loading -> {
                        playQueueImpl(clientId, items, 0)
                        MediaControllerGeneralState.Available
                    }
                    MediaControllerGeneralState.Available -> {
                        val mapped: List<QueueItem> = items.flatMap { it.toModel() }
                        _queue.update {
                            (it + mapped).apply { setKeys() }
                        }
                        MediaControllerGeneralState.Available
                    }
                }
            }
        }
    }

    fun playItem(clientId: Uuid, index: Int) {
        launchCommand {
            if (mediaPlayerState.value.isPlaying) {
                mediaPlayer.stop()
            }
            _queue.value[index].let { queueItem ->
                queueItem.audioId?.let {
                    if (_selectedClient.value == null) {
                        selectClientImpl(clientId)
                    }
                    mediaPlayer.setTrack(it)
                    mediaPlayer.play()
                }
            }
            _currentItemIndex.value = index
        }
    }

    fun changeItemIndex(oldIndex: Int, newIndex: Int) {
        launchCommand {
            _queue.update {
                it.toMutableList().apply {
                    add(newIndex, removeAt(oldIndex))
                }
            }
            _currentItemIndex.update { currentIndex ->
                if (currentIndex == oldIndex) {
                    newIndex
                } else {
                    if (currentIndex in min(oldIndex, newIndex)..max(oldIndex, newIndex)) {
                        if (oldIndex < newIndex) currentIndex - 1
                        else if (oldIndex > newIndex) currentIndex + 1
                        else currentIndex
                    } else {
                        currentIndex
                    }
                }
            }
        }
    }

    fun removeItem(clientId: Uuid, key: Int) {
        launchCommand {
            val toBeRemoved = _queue.value.indexOfFirst { it.key == key }
            _queue.update { queue ->
                queue.toMutableList().apply {
                    removeAt(toBeRemoved)
                }
            }
            _currentItemIndex.update { current ->
                if (toBeRemoved < current) current - 1
                else if (toBeRemoved > current) current
                else {
                    val wasPlaying = mediaPlayerState.value.isPlaying
                    if (wasPlaying) {
                        mediaPlayer.stop()
                    }
                    val queue = _queue.value
                    for (i in toBeRemoved ..< queue.size) {
                        val audioId = queue[i].audioId
                        if (audioId != null) {
                            if (_selectedClient.value == null) {
                                selectClientImpl(clientId)
                            }
                            mediaPlayer.setTrack(audioId)
                            if (wasPlaying) {
                                mediaPlayer.play()
                            }
                            break
                        }
                    }
                    current
                }
            }
        }
    }

    private suspend fun selectClientImpl(id: Uuid?) {
        mediaPlayer.setClient(id?.let { clientManager.get(it) })
        _selectedClient.value = id
    }

    private suspend fun playQueueImpl(clientId: Uuid, queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
        if (_selectedClient.value == null) {
            selectClientImpl(clientId)
        }
        val mapped: List<QueueItem> = queue.flatMap { it.toModel() }
        mapped[queueItemIndex].let { queueItem ->
            queueItem.audioId?.let {
                mediaPlayer.setTrack(it)
                mediaPlayer.play()
            } ?: TODO()
        }
        _queue.value = mapped.apply { setKeys() }
        _currentItemIndex.value = queueItemIndex
    }

    private suspend fun _next(clientId: Uuid?) {
        if (_queue.value.isEmpty()) { TODO() }
        if (_selectedClient.value == null) {
            selectClientImpl(clientId)
        }
        if (mediaPlayerState.value.isPlaying) { mediaPlayer.stop() }
        val queue = _queue.value
        val currentIndex = _currentItemIndex.value
        val newIndex = if (currentIndex < queue.lastIndex) currentIndex + 1 else 0
        _currentItemIndex.value = newIndex
        val newTrack = queue[newIndex]
        newTrack.audioId?.let {
            mediaPlayer.setTrack(it)
            mediaPlayer.play()
        }
    }

    private suspend fun QueueItemParameter.toModel(): List<QueueItem> {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.get(this.id).first().let { dbTrack ->
                dbTrack?.let { listOf(dbTrack.toQueueItem()) } ?: emptyList() // TODO
            }
            is QueueItemParameter.Playlist -> trackRepo.getPlaylistTracks(this.id).first().map { dbTrack ->
                dbTrack.toQueueItem()
            }
            is QueueItemParameter.Artist -> trackRepo.getArtistTracks(this.id).first().map { dbTrack ->
                if (dbTrack.spotifyTrack == null) TODO()
                dbTrack.toQueueItem()
            }
            is QueueItemParameter.Album -> trackRepo.getAlbumTracks(this.id).first().map { dbTrack ->
                if (dbTrack.spotifyTrack == null) TODO()
                dbTrack.toQueueItem()
            }
        }
    }

    private suspend fun TrackRelation.toQueueItem(): QueueItem {
        return QueueItem(
            id = track.id,
            name = spotifyTrack?.name ?: this.originalImport.title,
            album = run {
                spotifyTrack?.let { sp ->
                    albumRepo.get(sp.spotifyAlbumId).first()?.let {
                        QueueItem.Album.SpotifyAlbum(it.id, it.name)
                    }
                } ?: QueueItem.Album.ImportAlbum(this.originalImport.album)
            },
            imageId = run {
                if (spotifyTrack != null) {
                    mediaFileRepo.getSpotifyAlbumImage(spotifyTrack.spotifyAlbumId)
                } else {
                    mediaFileRepo.getImportSessionItemImage(track.importSessionItemId)
                }
            },
            artists = getArtists(this),
            audioId = mediaFileRepo.getImportSessionItemAudio(this.track.importSessionItemId),
            durationMs = this.originalImport.durationMilliseconds
        )
    }

    private suspend fun getArtists(dbTrack: TrackRelation): QueueItem.Artists {
        return if (dbTrack.spotifyTrack != null) {
            QueueItem.Artists.SpotifyArtists(
                list = artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack.id).first().map { dbArtist ->
                    Pair(dbArtist.id, dbArtist.name)
                }
            )
        } else {
            QueueItem.Artists.ImportArtist(
                list = dbTrack.originalImport.inspection.artists
            )
        }
    }

    private fun List<QueueItem>.setKeys() {
        this.forEachIndexed { index, item ->
            if (item.key == null) {
                item.key = index
            }
        }
    }

    private inline fun launchCommand(crossinline block: suspend () -> Unit) {
        coroutineScope.launch {
            mutex.withLock {
                _enabled.value = false
                block()
                if (_queue.value.isNotEmpty()) {
                    _enabled.value = true
                } else {
                    _state.value = MediaControllerGeneralState.Unavailable
                }
            }
        }
    }

    suspend fun release() {
        coroutineScope.cancel()
        mediaPlayer.release()
    }

    private class MediaPlayerImpl : MediaPlayer() {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val logger = KotlinLogging.logger {}
        private val _state: MutableStateFlow<MediaPlayerState> = MutableStateFlow(MediaPlayerState.initial())
        @Volatile private var observeJob: Job? = null
        @Volatile private var client: Client? = null

        override val state: StateFlow<MediaPlayerState> = _state.asStateFlow()

        suspend fun setClient(client: Client?) {
            observeJob?.cancelAndJoin()
            observeJob = null
            val previousState = _state.value
            val newStartPosition = previousState.positionMs
            try {
                this.client?.call<Unit>(MediaPlayerRpc.Stop)
            // this does not work as expected (handling CancellationException of this function)
            // it seems if the websocket session is already closed it throws CancellationException, which is not desired in this case
//            } catch (e: CancellationException) {
//                throw e
            } catch (e: Exception) {
                logger.error(e) { "could not stop playback in previous client" }
            }
            this.client = client
            setState(
                previousState.copy(startPositionMs = newStartPosition)
            )
            observeJob = coroutineScope.launch {
                client.let { client ->
                    try {
                        client?.observe<MediaPlayerState>(MediaPlayerRpc.ObserveState)?.collect(_state)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) {}
                        _state.update {
                            it.copy(isPlaying = false, startPositionMs = it.positionMs)
                        }
                    }
                }
            }
        }

        override suspend fun setTrack(id: MediaFileId) {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.SetTrack(id))
                } else {
                    _state.value = MediaPlayerState(track = id, isPlaying = false, startPositionMs = 0L, isFinished = false)
                }
            }
        }

        override suspend fun play() {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.Play)
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = it.positionMs) }
                }
            }
        }

        override suspend fun pause() {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.Pause)
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = it.positionMs) }
                }
            }
        }

        override suspend fun stop() {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.Stop)
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = 0L, isFinished = false) }
                }
            }
        }

        override suspend fun setTime(time: Duration) {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.SetTime(time.inWholeMilliseconds))
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = time.inWholeMilliseconds, isFinished = false) }
                }
            }
        }

        override suspend fun setState(value: MediaPlayerState) {
            client.let { client ->
                if (client != null) {
                    client.call<Unit>(MediaPlayerRpc.SetState(value))
                } else {
                    _state.update { value.copy(isPlaying = false) }
                }
            }
        }

        override suspend fun release() {
            coroutineScope.cancel()
        }
    }
}