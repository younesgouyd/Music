package dev.younesgouyd.apps.music.client

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class MediaController(
    private val mediaPlayer: MediaPlayer,
    private val repoStore: RepoStore
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()

    // UI STATE
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val queue: MutableStateFlow<List<MediaControllerState.Available.QueueItem>> = MutableStateFlow(emptyList())
    private val currentItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val timePositionChange: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    private val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val repeatState: MutableStateFlow<MediaControllerState.Available.RepeatState> =
        MutableStateFlow(MediaControllerState.Available.RepeatState.Off)
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)

    private val mediaFileRepo: MediaFileRepo get() = repoStore.mediaFileRepo
    private val trackRepo: TrackRepo get() = repoStore.trackRepo
    private val artistRepo: ArtistRepo get() = repoStore.artistRepo

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
                    next()
                }
            }
        )
    }

    fun playQueue(queue: List<QueueItemParameter>, queueItemIndex: Int = 0) {
        require(queue.isNotEmpty())
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
                val currentState = _state.value
                if (currentState is MediaControllerState.Unavailable || currentState is MediaControllerState.Loading) TODO()
                var wasPlaying: Boolean? = null
                if (isPlaying.value) {
                    wasPlaying = true
                    mediaPlayer.stop()
                } else {
                    wasPlaying = false
                }
                isPlaying.value = false
                val queue = queue.value
                val currentIndex = currentItemIndex.value
                val newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
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
                    val playingItemIsNotInvolved = currentIndex != oldIndex && currentIndex != newIndex
                    if (playingItemIsNotInvolved) {
                        currentIndex
                    } else {
                        if (currentIndex == oldIndex) { // dragged item is the one playing
                            newIndex
                        } else { // dragged item was placed at the index of playing item
                            if (oldIndex < newIndex) {
                                currentIndex - 1
                            } else {
                                currentIndex + 1
                            }
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    private suspend fun QueueItemParameter.toModel(): List<MediaControllerState.Available.QueueItem> {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.get(this.id).first().let { dbTrack ->
                listOf(
                    MediaControllerState.Available.QueueItem(
                        id = dbTrack.id,
                        name = dbTrack.name,
                        album = dbTrack.album,
                        image = mediaFileRepo.getTrackImage(dbTrack.id),
                        artists = artistRepo.getTrackArtists(this.id).first()
                            .map { dbArtist ->
                                MediaControllerState.Available.QueueItem.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = mediaFileRepo.getArtistImage(dbArtist.id)
                                )
                            },
                        uri = mediaFileRepo.getTrackAudioUri(dbTrack.id),
                        duration = dbTrack.duration
                    )
                )
            }
            is QueueItemParameter.Playlist -> trackRepo.getPlaylistTracks(this.id).first().map { dbTrack ->
                MediaControllerState.Available.QueueItem(
                    id = dbTrack.id,
                    name = dbTrack.name,
                    album = dbTrack.album,
                    image = mediaFileRepo.getTrackImage(dbTrack.id),
                    artists = artistRepo.getTrackArtists(dbTrack.id).first().map { dbArtist ->
                        MediaControllerState.Available.QueueItem.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name,
                            image = mediaFileRepo.getArtistImage(dbArtist.id)
                        )
                    },
                    uri = mediaFileRepo.getTrackAudioUri(dbTrack.id),
                    duration = dbTrack.duration
                )
            }
            is QueueItemParameter.Artist -> trackRepo.getArtistTracks(this.id).first().map { dbTrack ->
                MediaControllerState.Available.QueueItem(
                    id = dbTrack.id,
                    name = dbTrack.name,
                    album = dbTrack.album,
                    image = mediaFileRepo.getTrackImage(dbTrack.id),
                    artists = artistRepo.getTrackArtists(dbTrack.id).first().map { dbArtist ->
                        MediaControllerState.Available.QueueItem.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name,
                            image = mediaFileRepo.getArtistImage(dbArtist.id)
                        )
                    },
                    uri = mediaFileRepo.getTrackAudioUri(dbTrack.id),
                    duration = dbTrack.duration
                )
            }
        }
    }

    private fun List<MediaControllerState.Available.QueueItem>.setKeys() {
        val time = measureTime {
            this.mapIndexed { index, item ->
                if (item.key == null) {
                    item.key = index
                }
            }
        }
        println("::setKeys | took: $time")
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
            val currentItem: StateFlow<QueueItem> = combine(queue, queueItemIndex) { queue, index ->
                Pair(queue, index)
            }.mapLatest { (queue, index) ->
                queue[index]
            }.stateIn(coroutineScope, SharingStarted.Companion.Lazily, queue.value[queueItemIndex.value])

            enum class RepeatState { Off, List, Track }

            data class QueueItem(
                var key: Int? = null,
                val id: TrackId,
                val name: String,
                val album: String?,
                val image: File?,
                val artists: List<Artist>,
                val uri: String?,
                val duration: Duration?
            ) {
                data class Artist(
                    val id: ArtistId,
                    val name: String,
                    val image: File?
                )
            }
        }
    }

    sealed class QueueItemParameter {
        data class Track(val id: TrackId) : QueueItemParameter()
        data class Playlist(val id: PlaylistId) : QueueItemParameter()
        data class Artist(val id: ArtistId) : QueueItemParameter()
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