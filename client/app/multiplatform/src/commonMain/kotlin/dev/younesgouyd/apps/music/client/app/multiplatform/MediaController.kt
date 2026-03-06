package dev.younesgouyd.apps.music.client.app.multiplatform

import dev.younesgouyd.apps.music.client.app.multiplatform.data.PlaylistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.SpotifyAlbumRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.SpotifyArtistRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.TrackRelation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MediaController(
    private val mediaPlayer: MediaPlayer,
    private val mediaFileRepo: MediaFileRepo,
    private val trackRepo: TrackRepo,
    private val artistRepo: SpotifyArtistRepo,
    private val albumRepo: SpotifyAlbumRepo
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {  }

    // UI STATE
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val queue: MutableStateFlow<List<MediaControllerState.Available.QueueItem>> = MutableStateFlow(emptyList())
    private val currentItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val timePositionChange: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    private val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val repeatState: MutableStateFlow<MediaControllerState.Available.RepeatState> = MutableStateFlow(MediaControllerState.Available.RepeatState.Off)
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)

    val state: StateFlow<MediaControllerState> get() = _state.asStateFlow()

    init {
        mediaPlayer.registerEventListener(
            object : MediaPlayer.EventListener() {
                override fun onPlaying() {
                    isPlaying.value = true
                }

                override fun onPaused() {
                    isPlaying.value = false
                }

                override fun onStopped() {
                    isPlaying.value = false
                }

                override fun onTimePositionChange(time: Duration) {
                    timePositionChange.value = time
                }

                override fun onFinished() {
                    coroutineScope.launch {
                        mutex.withLock {
                            enabled.value = false
                            when (repeatState.value) {
                                MediaControllerState.Available.RepeatState.Off -> {
                                    val currentState = _state.value
                                    if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                                    if (queue.value.isEmpty()) { TODO() }
                                    val wasPlaying = isPlaying.value
                                    if (wasPlaying) { mediaPlayer.stop() }
                                    isPlaying.value = false
                                    timePositionChange.value = 0.milliseconds
                                    val queue = queue.value
                                    val currentIndex = currentItemIndex.value
                                    if (currentIndex < queue.lastIndex) {
                                        val newIndex = currentIndex + 1
                                        currentItemIndex.value = newIndex
                                        val newTrack = queue[newIndex]
                                        if (newTrack.uri != null) {
                                            mediaPlayer.setMedia(newTrack.uri)
                                            if (wasPlaying) {
                                                mediaPlayer.play()
                                            }
                                        }
                                        isPlaying.value = wasPlaying
                                    }
                                }
                                MediaControllerState.Available.RepeatState.List -> {
                                    _next()
                                }
                                MediaControllerState.Available.RepeatState.Track -> {
                                    val currentState = _state.value
                                    if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                                    if (queue.value.isEmpty()) { TODO() }
                                    val wasPlaying = isPlaying.value
                                    if (wasPlaying) { mediaPlayer.stop() }
                                    isPlaying.value = false
                                    val queue = queue.value
                                    timePositionChange.value = 0.milliseconds
                                    val track = queue[currentItemIndex.value]
                                    if (track.uri != null) {
                                        mediaPlayer.setMedia(track.uri)
                                        if (wasPlaying) {
                                            mediaPlayer.play()
                                        }
                                    }
                                    isPlaying.value = wasPlaying
                                }
                            }
                            enabled.value = true
                        }
                    }
                }
            }
        )
    }

    fun playQueue(queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
        if (queue.isEmpty()) {
            logger.warn { "::playQueue | queue is empty" }
            return
        }
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> MediaControllerState.Loading
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) {
                                mediaPlayer.pause()
                            }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading, is MediaControllerState.Available -> {
                            val mapped: List<MediaControllerState.Available.QueueItem> = queue.flatMap { it.toModel() }
                            mapped[queueItemIndex].let { queueItem ->
                                if (queueItem.uri != null) {
                                    mediaPlayer.setMedia(queueItem.uri)
                                    timePositionChange.value = 0.milliseconds
                                    mediaPlayer.play()
                                    isPlaying.value = true
                                } else {
                                    TODO()
                                }
                            }
                            this@MediaController.queue.value = mapped.apply { setKeys() }
                            this@MediaController.currentItemIndex.value = queueItemIndex
                            MediaControllerState.Available(
                                enabled = this@MediaController.enabled.asStateFlow(),
                                queue = this@MediaController.queue.asStateFlow(),
                                queueItemIndex = this@MediaController.currentItemIndex.asStateFlow(),
                                timePositionChange = this@MediaController.timePositionChange.asStateFlow(),
                                isPlaying = this@MediaController.isPlaying.asStateFlow(),
                                repeatState = this@MediaController.repeatState.asStateFlow()
                            )
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun play() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (!currentState.isPlaying.value) {
                                mediaPlayer.play()
                            }
                            isPlaying.value = true
                            currentState
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun pause() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) {
                                mediaPlayer.pause()
                            }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun seek(position: Duration) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                mediaPlayer.setTime(position)
                timePositionChange.value = position
                this@MediaController.enabled.value = true
            }
        }
    }

    fun next() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _next()
                this@MediaController.enabled.value = true
            }
        }
    }

    fun previous() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                var wasPlaying: Boolean? = null
                if (isPlaying.value) {
                    wasPlaying = true; mediaPlayer.stop()
                } else {
                    wasPlaying = false
                }
                isPlaying.value = false
                val queue = queue.value
                val currentIndex = currentItemIndex.value
                val newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                val newTrack = queue[newIndex]
                if (newTrack.uri != null) {
                    mediaPlayer.setMedia(newTrack.uri)
                    timePositionChange.value = 0.milliseconds
                    if (wasPlaying) {
                        mediaPlayer.play()
                    }
                }
                isPlaying.value = wasPlaying
                currentItemIndex.value = newIndex
                this@MediaController.enabled.value = true
            }
        }
    }

    fun repeat() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                repeatState.update {
                    when (it) {
                        MediaControllerState.Available.RepeatState.Off -> MediaControllerState.Available.RepeatState.List
                        MediaControllerState.Available.RepeatState.List -> MediaControllerState.Available.RepeatState.Track
                        MediaControllerState.Available.RepeatState.Track -> MediaControllerState.Available.RepeatState.Off
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun addToQueue(items: List<QueueItemParameter>) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                when (_state.value) {
                    is MediaControllerState.Unavailable -> playQueue(items, 0)
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        val mapped: List<MediaControllerState.Available.QueueItem> = items.flatMap { it.toModel() }
                        queue.update {
                            (it + mapped).apply { setKeys() }
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun playItem(index: Int) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                if (isPlaying.value) {
                    mediaPlayer.stop()
                }
                isPlaying.value = false
                queue.value[index].let { queueItem ->
                    if (queueItem.uri != null) {
                        mediaPlayer.setMedia(queueItem.uri)
                        timePositionChange.value = 0.milliseconds
                        mediaPlayer.play()
                        isPlaying.value = true
                    }
                }
                currentItemIndex.value = index
                this@MediaController.enabled.value = true
            }
        }
    }

    fun changeItemIndex(oldIndex: Int, newIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                queue.update {
                    it.toMutableList().apply {
                        add(newIndex, removeAt(oldIndex))
                    }
                }
                currentItemIndex.update { currentIndex ->
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
                this@MediaController.enabled.value = true
            }
        }
    }

    fun removeItem(key: Int) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                val toBeRemoved = queue.value.indexOfFirst { it.key == key }
                queue.update { queue ->
                    queue.toMutableList().apply {
                        removeAt(toBeRemoved)
                    }
                }
                currentItemIndex.update { current ->
                    if (toBeRemoved < current) current - 1
                    else if (toBeRemoved > current) current
                    else {
                        tryPlayFrom(toBeRemoved)
                        current
                    }
                }
                if (queue.value.isNotEmpty()) {
                    this@MediaController.enabled.value = true
                }
            }
        }
    }

    private suspend fun _next() {
        val currentState = _state.value
        if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
        if (queue.value.isEmpty()) { TODO() }
        val wasPlaying = isPlaying.value
        if (wasPlaying) { mediaPlayer.stop() }
        isPlaying.value = false
        val queue = queue.value
        val currentIndex = currentItemIndex.value
        val newIndex = if (currentIndex < queue.lastIndex) currentIndex + 1 else 0
        currentItemIndex.value = newIndex
        timePositionChange.value = 0.milliseconds
        val newTrack = queue[newIndex]
        if (newTrack.uri != null) {
            mediaPlayer.setMedia(newTrack.uri)
            if (wasPlaying) {
                mediaPlayer.play()
            }
        }
        isPlaying.value = wasPlaying
    }

    private suspend fun tryPlayFrom(index: Int) {
        withContext(Dispatchers.Default) {
            timePositionChange.value = 0.milliseconds
            val wasPlaying = isPlaying.value
            if (wasPlaying) {
                mediaPlayer.stop()
                isPlaying.value = false
            }
            val queue = queue.value
            for (i in index ..< queue.size) {
                val uri = queue[i].uri
                if (uri != null) {
                    mediaPlayer.setMedia(uri)
                    if (wasPlaying) {
                        mediaPlayer.play()
                        isPlaying.value = true
                    }
                    break
                }
            }
        }
    }

    private suspend fun QueueItemParameter.toModel(): List<MediaControllerState.Available.QueueItem> {
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

    private suspend fun TrackRelation.toQueueItem(): MediaControllerState.Available.QueueItem {
        return MediaControllerState.Available.QueueItem(
            id = this.track.id,
            name = this.spotifyTrack?.name ?: this.originalImport.title,
            album = run {
                this.spotifyTrack?.let {
                    albumRepo.get(this.spotifyTrack.spotifyAlbumId).first()?.let {
                        MediaControllerState.Available.QueueItem.Album.SpotifyAlbum(it.id, it.name)
                    }
                } ?: MediaControllerState.Available.QueueItem.Album.ImportAlbum(this.originalImport.album)
            },
            image = getImage(this),
            artists = getArtists(this),
            uri = mediaFileRepo.getImportSessionItemAudioUri(this.track.importSessionItemId),
            duration = this.originalImport.durationMilliseconds.milliseconds
        )
    }

    private suspend fun getImage(dbTrack: TrackRelation): File? {
        return if (dbTrack.spotifyTrack != null) {
            mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack.spotifyAlbumId)
        } else {
            mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
        }
    }

    private suspend fun getArtists(dbTrack: TrackRelation): MediaControllerState.Available.QueueItem.Artists {
        return if (dbTrack.spotifyTrack != null) {
            MediaControllerState.Available.QueueItem.Artists.SpotifyArtists(
                list = artistRepo.getSpotifyTrackSpotifyArtists(dbTrack.spotifyTrack.id).first().map { dbArtist ->
                    Pair(dbArtist.id, dbArtist.name)
                }
            )
        } else {
            MediaControllerState.Available.QueueItem.Artists.ImportArtist(
                list = dbTrack.originalImport.inspection.artists
            )
        }
    }

    private fun List<MediaControllerState.Available.QueueItem>.setKeys() {
        this.forEachIndexed { index, item ->
            if (item.key == null) {
                item.key = index
            }
        }
    }

    fun release() {
        coroutineScope.cancel()
        mediaPlayer.release()
    }

    sealed class MediaControllerState {
        data object Loading : MediaControllerState()

        data object Unavailable : MediaControllerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val queue: StateFlow<List<QueueItem>>,
            val queueItemIndex: StateFlow<Int>,
            val timePositionChange: StateFlow<Duration>,
            val isPlaying: StateFlow<Boolean>,
            val repeatState: StateFlow<RepeatState>
        ) : MediaControllerState() {
            private val coroutineScope = CoroutineScope(Dispatchers.Main)

            @OptIn(ExperimentalCoroutinesApi::class)
            val currentItem: StateFlow<QueueItem?> = combine(queue, queueItemIndex) { queue, index ->
                Pair(queue, index)
            }.mapLatest { (queue, index) ->
                if (queue.isNotEmpty()) { queue[index] } else { null }
            }.stateIn(coroutineScope, SharingStarted.Lazily, null)

            enum class RepeatState { Off, List, Track }

            data class QueueItem(
                var key: Int? = null,
                val id: TrackId,
                val name: String,
                val album: Album,
                val image: File?,
                val artists: Artists,
                val uri: String?,
                val duration: Duration?
            ) {
                sealed class Album {
                    data class SpotifyAlbum(
                        val id: SpotifyAlbumId,
                        val name: String
                    ) : Album()

                    data class ImportAlbum(
                        val name: String?
                    ) : Album()
                }

                sealed class Artists {
                    data class SpotifyArtists(
                        val list: List<Pair<SpotifyArtistId, String>>
                    ) : Artists()

                    data class ImportArtist(
                        val list: List<String>
                    ) : Artists()
                }
            }
        }
    }

    sealed class QueueItemParameter {
        data class Track(val id: TrackId) : QueueItemParameter()
        data class Playlist(val id: PlaylistId) : QueueItemParameter()
        data class Artist(val id: SpotifyArtistId) : QueueItemParameter()
        data class Album(val id: SpotifyAlbumId) : QueueItemParameter()
    }

    abstract class MediaPlayer {
        abstract fun registerEventListener(eventListener: EventListener)
        abstract fun setMedia(uri: String)
        abstract fun play()
        abstract fun pause()
        abstract fun stop()
        abstract fun setTime(time: Duration)
        abstract fun release()

        abstract class EventListener {
            abstract fun onPlaying()
            abstract fun onPaused()
            abstract fun onStopped()
            abstract fun onTimePositionChange(time: Duration)
            abstract fun onFinished()
        }
    }
}