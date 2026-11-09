package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackContext
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.ClientRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.TrackRelation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min
import dev.younesgouyd.apps.music.common.models.datamodels.Client as ClientModel

class MediaController(
    clientRepo: ClientRepo,
    websocketSessionHandlers: WebSocketSessionHandlers,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val folderRepo: FolderRepo
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}

    private val _selectedClient: MutableStateFlow<ClientId?> = MutableStateFlow(null)
    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _queue: MutableStateFlow<List<QueueItem>> = MutableStateFlow(emptyList())
    private val _currentItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val currentItem: QueueItem?
        get() {
            val queue = _queue.value
            return if (queue.isNotEmpty()) {
                queue[_currentItemIndex.value]
            } else {
                null
            }
        }
    private val _repeatState: MutableStateFlow<RepeatState> = MutableStateFlow(RepeatState.Off)

    private val _state: MutableStateFlow<MediaControllerGeneralState> = MutableStateFlow(MediaControllerGeneralState.Unavailable)

    private val mediaPlayer: MediaPlayerImpl = MediaPlayerImpl(websocketSessionHandlers)

    val clients: StateFlow<List<ClientModel>> = clientRepo.getConnected()
        .map { it.map { ClientModel(it.id, it.name, it.deviceType) } } // TODO
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val selectedClient: StateFlow<ClientId?> = _selectedClient.asStateFlow()
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
                                    _selectedClient.value?.let { clientId ->
                                        mediaPlayer.play(PlaybackContext.ServerSent(newTrack))
                                    }
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
                                mediaPlayer.play(PlaybackContext.ServerSent(track))
                            }
                        }
                    }
                    _enabled.value = true
                }
            }
        }
    }

    suspend fun selectClient(id: ClientId?) {
        launchCommand {
            selectClientImpl(id)
        }
    }

    suspend fun playQueue(clientId: ClientId, queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
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
                        if (_queue.value.isNotEmpty()) {
                            MediaControllerGeneralState.Available
                        } else {
                            MediaControllerGeneralState.Unavailable
                        }
                    }
                }
            }
        }
    }

    suspend fun play(clientId: ClientId) {
        launchCommand {
            if (!mediaPlayerState.value.isPlaying) {
                if (_selectedClient.value == null) {
                    selectClientImpl(clientId)
                }
                currentItem?.let { track ->
                    mediaPlayer.play(PlaybackContext.ServerSent(track))
                }
            }
        }
    }

    suspend fun pause() {
        launchCommand {
            if (mediaPlayerState.value.isPlaying) {
                mediaPlayer.pause()
            }
        }
    }

    suspend fun seek(positionMs: Long) {
        launchCommand {
            currentItem?.let { track ->
                mediaPlayer.setPositionMs(
                    positionMs = positionMs,
                    context = PlaybackContext.ServerSent(track)
                )
            }
        }
        logger.debug { "<-- ::seek" }
    }

    suspend fun next(clientId: ClientId) {
        launchCommand {
            _next(clientId)
        }
    }

    suspend fun previous(clientId: ClientId) {
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
                    mediaPlayer.play(PlaybackContext.ServerSent(newTrack))
                }
            }
            _currentItemIndex.value = newIndex
        }
    }

    suspend fun repeat() {
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

    suspend fun addToQueue(clientId: ClientId, items: List<QueueItemParameter>) {
        launchCommand {
            if (_state.value == MediaControllerGeneralState.Unavailable) {
                _state.value = MediaControllerGeneralState.Loading
            }
            _state.update { currentState ->
                when (currentState) {
                    MediaControllerGeneralState.Unavailable -> TODO()
                    MediaControllerGeneralState.Loading -> {
                        playQueueImpl(clientId, items, 0)
                        if (_queue.value.isNotEmpty()) {
                            MediaControllerGeneralState.Available
                        } else {
                            MediaControllerGeneralState.Unavailable
                        }
                    }
                    MediaControllerGeneralState.Available -> {
                        val mapped: List<QueueItem> = items.flatMap { it.toModel() }
                        if (mapped.isEmpty()) {
                            currentState
                        } else {
                            _queue.update {
                                (it + mapped).apply { setKeys() }
                            }
                            MediaControllerGeneralState.Available
                        }
                    }
                }
            }
        }
    }

    suspend fun playItem(clientId: ClientId, index: Int) {
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
                    mediaPlayer.play(PlaybackContext.ServerSent(queueItem))
                }
            }
            _currentItemIndex.value = index
        }
    }

    suspend fun changeItemIndex(oldIndex: Int, newIndex: Int) {
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

    suspend fun removeItem(clientId: ClientId, key: Int) {
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
                        val queueItem = queue[i]
                        val audioId = queueItem.audioId
                        if (audioId != null) {
                            if (_selectedClient.value == null) {
                                selectClientImpl(clientId)
                            }
                            mediaPlayer.setTrack(audioId)
                            if (wasPlaying) {
                                mediaPlayer.play(PlaybackContext.ServerSent(queueItem))
                            }
                            break
                        }
                    }
                    current
                }
            }
        }
    }

    private suspend fun selectClientImpl(id: ClientId?) {
        mediaPlayer.setClient(
            id = id,
            context = currentItem?.let { queueItem ->
                PlaybackContext.ServerSent(queueItem)
            }
        )
        _selectedClient.value = id
    }

    private suspend fun playQueueImpl(clientId: ClientId, queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
        if (_selectedClient.value == null) {
            selectClientImpl(clientId)
        }
        val mapped: List<QueueItem> = queue.flatMap { it.toModel() }
        if (mapped.isEmpty()) return
        mapped[queueItemIndex].let { queueItem ->
            queueItem.audioId?.let {
                mediaPlayer.setTrack(it)
                mediaPlayer.play(PlaybackContext.ServerSent(queueItem))
            } ?: TODO()
        }
        _queue.value = mapped.apply { setKeys() }
        _currentItemIndex.value = queueItemIndex
    }

    private suspend fun _next(clientId: ClientId?) {
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
            mediaPlayer.play(PlaybackContext.ServerSent(newTrack))
        }
    }

    private suspend fun QueueItemParameter.toModel(): List<QueueItem> {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.get(this.id).first().let { dbTrack ->
                dbTrack?.let { listOf(dbTrack.toQueueItem(QueueItem.AddedFrom.None)) } ?: emptyList() // TODO
            }
            is QueueItemParameter.Playlist -> trackRepo.getPlaylistTracks(this.id).first().map { dbTrack ->
                dbTrack.toQueueItem(QueueItem.AddedFrom.Playlist(this.id))
            }
            is QueueItemParameter.Folder -> {
                suspend fun getFolderItems(_id: FolderId): List<QueueItem> {
                    val tracks = trackRepo.getFolderTracks(_id).first()
                        .map { dbTrack -> dbTrack.toQueueItem(QueueItem.AddedFrom.Folder(this.id)) }
                    val playlists = playlistRepo.getFolderPlaylists(_id).first()
                        .flatMap { dbPlaylist ->
                            trackRepo.getPlaylistTracks(dbPlaylist.id).first().map { dbTrack ->
                                dbTrack.toQueueItem(QueueItem.AddedFrom.Folder(this.id))
                            }
                        }
                    return tracks + playlists + folderRepo.getSubfolders(_id).first().flatMap { getFolderItems(it.id) }
                }
                getFolderItems(this.id)
            }
            is QueueItemParameter.Artist -> trackRepo.getArtistTracks(this.id).first().map { dbTrack ->
                if (dbTrack.spotifyTrackRelation == null) TODO()
                dbTrack.toQueueItem(QueueItem.AddedFrom.Artist(this.id))
            }
            is QueueItemParameter.Album -> trackRepo.getAlbumTracks(this.id).first().map { dbTrack ->
                if (dbTrack.spotifyTrackRelation == null) TODO()
                dbTrack.toQueueItem(QueueItem.AddedFrom.Album(this.id))
            }
        }
    }

    private fun TrackRelation.toQueueItem(addedFrom: QueueItem.AddedFrom): QueueItem {
        return QueueItem(
            id = track.id,
            importId = this.originalImport.id,
            name = spotifyTrackRelation?.spotifyTrack?.name ?: this.originalImport.title,
            album = spotifyTrackRelation?.spotifyAlbum?.let { QueueItem.Album.SpotifyAlbum(it.id, it.name) } ?: QueueItem.Album.ImportAlbum(this.originalImport.album),
            imageId = spotifyTrackRelation?.spotifyAlbum?.mediumImgId ?: originalImport.imgId,
            artists = run {
                if (spotifyTrackRelation != null) {
                    QueueItem.Artists.SpotifyArtists(
                        spotifyTrackRelation.spotifyArtists.map { Pair(it.id, it.name) }
                    )
                } else {
                    QueueItem.Artists.ImportArtist(originalImport.inspection.artists)
                }
            },
            audioId = originalImport.audioFileId,
            durationMs = this.originalImport.durationMilliseconds,
            addedFrom = addedFrom
        )
    }

    private fun List<QueueItem>.setKeys() {
        this.forEachIndexed { index, item ->
            if (item.key == null) {
                item.key = index
            }
        }
    }

    private suspend inline fun launchCommand(crossinline block: suspend () -> Unit) {
        withContext(Dispatchers.Default) {
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

    suspend fun release() { // TODO
        coroutineScope.cancel()
        mediaPlayer.release()
    }

    private class MediaPlayerImpl(
        private val websocketSessionHandlers: WebSocketSessionHandlers
    ) : MediaPlayer() {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val logger = KotlinLogging.logger {}
        private val _state: MutableStateFlow<MediaPlayerState> = MutableStateFlow(MediaPlayerState.initial())
        @Volatile private var observeJob: Job? = null
        @Volatile private var client: WebSocketSessionHandler? = null
        private val playbackIntervalsChannel = Channel<PlaybackInterval>(Channel.UNLIMITED)

        override val state: StateFlow<MediaPlayerState> = _state.asStateFlow()

        suspend fun setClient(id: ClientId?, context: PlaybackContext.ServerSent?) {
            val client: WebSocketSessionHandler? = id?.let { websocketSessionHandlers.get(it) }
            observeJob?.cancelAndJoin()
            observeJob = null
            val previousState = _state.value
            val newStartPosition = previousState.positionMs
            try {
                this.client?.call(Rpc.ToClient.MediaPlayer.Stop)
            } catch (e: Exception) {
                logger.error(e) { "could not stop playback in previous client" }
            }
            this.client = client
            if (context == null) {
                setState(
                    value = MediaPlayerState.initial(),
                    context = null
                )
            } else {
                setState(
                    value = previousState.copy(startPositionMs = newStartPosition),
                    context = context
                )
            }
            observeJob = coroutineScope.launch {
                client.let { client ->
                    try {
                        client?.observe(Rpc.ToClient.MediaPlayer.ObserveState)?.collect(_state)
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
                    client.call(Rpc.ToClient.MediaPlayer.SetTrack(id))
                } else {
                    _state.value = MediaPlayerState(track = id, isPlaying = false, startPositionMs = 0L, isFinished = false)
                }
            }
        }

        override suspend fun play(context: PlaybackContext.ServerSent) {
            client.let { client ->
                if (client != null) {
                    client.call(Rpc.ToClient.MediaPlayer.Play(context))
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = it.positionMs) }
                }
            }
        }

        override suspend fun pause() {
            client.let { client ->
                if (client != null) {
                    client.call(Rpc.ToClient.MediaPlayer.Pause)
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = it.positionMs) }
                }
            }
        }

        override suspend fun stop() {
            client.let { client ->
                if (client != null) {
                    client.call(Rpc.ToClient.MediaPlayer.Stop)
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = 0L, isFinished = false) }
                }
            }
        }

        override suspend fun setPositionMs(positionMs: Long, context: PlaybackContext.ServerSent) {
            client.let { client ->
                if (client != null) {
                    client.call(Rpc.ToClient.MediaPlayer.SetTime(positionMs, context))
                } else {
                    _state.update { it.copy(isPlaying = false, startPositionMs = positionMs, isFinished = false) }
                }
            }
        }

        override suspend fun setState(value: MediaPlayerState, context: PlaybackContext.ServerSent?) {
            client.let { client ->
                if (client != null) {
                    client.call(Rpc.ToClient.MediaPlayer.SetState(value, context))
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