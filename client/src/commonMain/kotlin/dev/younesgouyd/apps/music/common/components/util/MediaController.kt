package dev.younesgouyd.apps.music.common.components.util

import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.repoes.AlbumRepo
import dev.younesgouyd.apps.music.common.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.common.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.common.data.repoes.TrackRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaController(
    private val mediaPlayer: MediaPlayer,
    private val repoStore: RepoStore
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val timePositionChange: MutableStateFlow<Long> = MutableStateFlow(0)

    private val trackRepo: TrackRepo get() = repoStore.trackRepo
    private val artistRepo: ArtistRepo get() = repoStore.artistRepo
    private val albumRepo: AlbumRepo get() = repoStore.albumRepo
    private val playlistRepo: PlaylistRepo get() = repoStore.playlistRepo

    val state: StateFlow<MediaControllerState> get() = _state.asStateFlow()

    init {
        mediaPlayer.registerEventListener(
            object : MediaPlayer.EventListener() {
                override fun onPlaying() { isPlaying.value = true }
                override fun onPaused() { isPlaying.value = false }
                override fun onStopped() { isPlaying.value = false }
                override fun onTimePositionChange(time: Long) { timePositionChange.value = time }
                override fun onFinished() { next() }
            }
        )
    }

    fun playQueue(queue: List<QueueItemParameter>, queueItemIndex: Int = 0, queueSubItemIndex: Int = 0) {
        require(queue.isNotEmpty())
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> MediaControllerState.Loading
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) { mediaPlayer.pause() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading, is MediaControllerState.Available -> {
                            val mapped: List<MediaControllerState.Available.QueueItem> = queue.map { it.toModel() }
                            mapped[queueItemIndex].let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> firstQueueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> firstQueueItem.items[queueSubItemIndex] // TODO: handle invalid subIndex
                                    is MediaControllerState.Available.QueueItem.Album -> firstQueueItem.items[queueSubItemIndex] // TODO: handle invalid subIndex
                                }
                                if (currentTrack.uri != null) {
                                    mediaPlayer.setMedia(currentTrack.uri)
                                    timePositionChange.value = 0
                                    mediaPlayer.play()
                                    isPlaying.value = true
                                } else {
                                    TODO()
                                }
                            }
                            when (currentState) {
                                is MediaControllerState.Loading -> {
                                    MediaControllerState.Available(
                                        enabled = this@MediaController.enabled.asStateFlow(),
                                        queue = mapped,
                                        queueItemIndex = queueItemIndex,
                                        queueSubItemIndex = queueSubItemIndex,
                                        timePositionChange = timePositionChange,
                                        isPlaying = isPlaying.asStateFlow(),
                                        repeatState = MediaControllerState.Available.RepeatState.Off,
                                    )
                                }
                                is MediaControllerState.Available -> {
                                    currentState.copy(
                                        queue = mapped,
                                        queueItemIndex = queueItemIndex,
                                        queueSubItemIndex = queueSubItemIndex
                                    )
                                }
                                else -> { TODO() }
                            }
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
                            if (!currentState.isPlaying.value) { mediaPlayer.play() }
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
                            if (currentState.isPlaying.value) { mediaPlayer.pause() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun seek(position: Long) {
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
                var wasPlaying: Boolean? = null
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) { wasPlaying = true; mediaPlayer.stop() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            val queue = currentState.queue
                            val currentIndex = currentState.queueItemIndex
                            val currentSubIndex = currentState.queueSubItemIndex
                            val newIndex: Int
                            val newSubIndex: Int
                            queue[currentIndex].let { queueItem ->
                                when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> {
                                        newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                        newSubIndex = 0
                                    }
                                    is MediaControllerState.Available.QueueItem.Playlist -> {
                                        if (currentSubIndex + 1 > queueItem.items.size - 1) {
                                            newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                            newSubIndex = 0
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex + 1
                                        }
                                    }
                                    is MediaControllerState.Available.QueueItem.Album -> {
                                        if (currentSubIndex + 1 > queueItem.items.size - 1) {
                                            newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                            newSubIndex = 0
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex + 1
                                        }
                                    }
                                }
                            }
                            val newTrack = queue[newIndex].let { queueItem ->
                                when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> queueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                    is MediaControllerState.Available.QueueItem.Album -> queueItem.items[newSubIndex]
                                }
                            }
                            if (wasPlaying == null) { TODO() }
                            if (newTrack.uri != null) {
                                mediaPlayer.setMedia(newTrack.uri)
                                timePositionChange.value = 0
                                if (wasPlaying) {
                                    mediaPlayer.play()
                                }
                            }
                            isPlaying.value = wasPlaying
                            currentState.copy(
                                queueItemIndex = newIndex,
                                queueSubItemIndex = newSubIndex
                            )
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun previous() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                var wasPlaying: Boolean? = null
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) { wasPlaying = true; mediaPlayer.stop() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            val queue = currentState.queue
                            val currentIndex = currentState.queueItemIndex
                            val currentSubIndex = currentState.queueSubItemIndex
                            val newIndex: Int
                            val newSubIndex: Int
                            queue[currentIndex].let { queueItem ->
                                when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> {
                                        newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                        newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                            when (it) {
                                                is MediaControllerState.Available.QueueItem.Track -> 0
                                                is MediaControllerState.Available.QueueItem.Playlist -> it.items.lastIndex
                                                is MediaControllerState.Available.QueueItem.Album -> it.items.lastIndex
                                            }
                                        }
                                    }
                                    is MediaControllerState.Available.QueueItem.Playlist -> {
                                        if (currentSubIndex - 1 < 0) {
                                            newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                            newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                                when (it) {
                                                    is MediaControllerState.Available.QueueItem.Track -> 0
                                                    is MediaControllerState.Available.QueueItem.Playlist -> it.items.lastIndex
                                                    is MediaControllerState.Available.QueueItem.Album -> it.items.lastIndex
                                                }
                                            }
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex - 1
                                        }
                                    }
                                    is MediaControllerState.Available.QueueItem.Album -> {
                                        if (currentSubIndex - 1 < 0) {
                                            newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                            newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                                when (it) {
                                                    is MediaControllerState.Available.QueueItem.Track -> 0
                                                    is MediaControllerState.Available.QueueItem.Playlist -> it.items.lastIndex
                                                    is MediaControllerState.Available.QueueItem.Album -> it.items.lastIndex
                                                }
                                            }
                                        } else {
                                            newIndex = currentIndex
                                            newSubIndex = currentSubIndex - 1
                                        }
                                    }
                                }
                            }
                            val newTrack = queue[newIndex].let { queueItem ->
                                when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> queueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                    is MediaControllerState.Available.QueueItem.Album -> queueItem.items[newSubIndex]
                                }
                            }
                            if (wasPlaying == null) TODO()
                            if (newTrack.uri != null) {
                                mediaPlayer.setMedia(newTrack.uri)
                                timePositionChange.value = 0
                                if (wasPlaying) { mediaPlayer.play() }
                            }
                            isPlaying.value = wasPlaying
                            currentState.copy(
                                queueItemIndex = newIndex,
                                queueSubItemIndex = newSubIndex
                            )
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun repeat() {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            currentState.copy(
                                repeatState = when (currentState.repeatState) {
                                    MediaControllerState.Available.RepeatState.Off -> MediaControllerState.Available.RepeatState.List
                                    MediaControllerState.Available.RepeatState.List -> MediaControllerState.Available.RepeatState.Track
                                    MediaControllerState.Available.RepeatState.Track -> MediaControllerState.Available.RepeatState.Off
                                }
                            )
                        }
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
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> MediaControllerState.Loading
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> currentState
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> {
                            val mapped: List<MediaControllerState.Available.QueueItem> = items.map { it.toModel() }
                            val newQueue = mapped
                            newQueue.first().let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> firstQueueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> firstQueueItem.items.first()
                                    is MediaControllerState.Available.QueueItem.Album -> firstQueueItem.items.first()
                                }
                                if (currentTrack.uri != null) {
                                    mediaPlayer.setMedia(currentTrack.uri)
                                    timePositionChange.value = 0
                                } else {
                                    TODO()
                                }
                            }
                            MediaControllerState.Available(
                                enabled = enabled.asStateFlow(),
                                queue = newQueue,
                                queueItemIndex = 0,
                                queueSubItemIndex = 0,
                                timePositionChange = timePositionChange,
                                isPlaying = this@MediaController.isPlaying,
                                repeatState = MediaControllerState.Available.RepeatState.Off
                            )
                        }
                        is MediaControllerState.Available -> {
                            val mapped: List<MediaControllerState.Available.QueueItem> = items.map { it.toModel() }
                            currentState.copy(queue = currentState.queue + mapped)
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun playQueueItem(queueItemIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) { mediaPlayer.stop() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            currentState.queue[queueItemIndex].let { queueItem ->
                                val newTrack = when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> queueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> queueItem.items.first()
                                    is MediaControllerState.Available.QueueItem.Album -> queueItem.items.first()
                                }
                                if (newTrack.uri != null) {
                                    mediaPlayer.setMedia(newTrack.uri)
                                    timePositionChange.value = 0
                                    mediaPlayer.play()
                                    isPlaying.value = true
                                }
                            }
                            currentState.copy(
                                queueItemIndex = queueItemIndex,
                                queueSubItemIndex = 0
                            )
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    fun playTrackInQueue(queueItemIndex: Int, trackIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                this@MediaController.enabled.value = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.isPlaying.value) { mediaPlayer.stop() }
                            isPlaying.value = false
                            currentState
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            currentState.queue[queueItemIndex].let { queueItem ->
                                val newTrack = when (queueItem) {
                                    is MediaControllerState.Available.QueueItem.Track -> queueItem
                                    is MediaControllerState.Available.QueueItem.Playlist -> queueItem.items[trackIndex]
                                    is MediaControllerState.Available.QueueItem.Album -> queueItem.items[trackIndex]
                                }
                                if (newTrack.uri != null) {
                                    mediaPlayer.setMedia(newTrack.uri)
                                    timePositionChange.value = 0
                                    mediaPlayer.play()
                                    isPlaying.value = true
                                }
                            }
                            currentState.copy(
                                queueItemIndex = queueItemIndex,
                                queueSubItemIndex = trackIndex
                            )
                        }
                    }
                }
                this@MediaController.enabled.value = true
            }
        }
    }

    private suspend fun QueueItemParameter.toModel(): MediaControllerState.Available.QueueItem {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.getStatic(this.id)!!.let { dbTrack ->
                MediaControllerState.Available.QueueItem.Track(
                    id = dbTrack.id,
                    name = dbTrack.name,
                    artists = artistRepo.getTrackArtistsStatic(this.id)
                        .map { dbArtist ->
                            MediaControllerState.Available.QueueItem.Track.Artist(
                                id = dbArtist.id,
                                name = dbArtist.name,
                                image = dbArtist.image
                            )
                        },
                    album = dbTrack.album_id?.let {
                        albumRepo.getStatic(it)!!.let { dbAlbum ->
                            MediaControllerState.Available.QueueItem.Track.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date
                            )
                        }
                    },
                    uri = dbTrack.audio_uri,
                    duration = dbTrack.duration
                )
            }
            is QueueItemParameter.Album -> albumRepo.getStatic(this.id)!!.let { dbAlbum ->
                MediaControllerState.Available.QueueItem.Album(
                    id = dbAlbum.id,
                    name = dbAlbum.name,
                    image = dbAlbum.image,
                    releaseDate = dbAlbum.release_date,
                    items = trackRepo.getAlbumTracksStatic(dbAlbum.id).map { dbTrack ->
                        MediaControllerState.Available.QueueItem.Track(
                            id = dbTrack.id,
                            name = dbTrack.name,
                            artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                MediaControllerState.Available.QueueItem.Track.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = dbArtist.image
                                )
                            },
                            album = MediaControllerState.Available.QueueItem.Track.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date
                            ),
                            uri = dbTrack.audio_uri,
                            duration = dbTrack.duration
                        )
                    }
                )
            }
            is QueueItemParameter.Playlist -> playlistRepo.getStatic(this.id)!!.let { dbPlaylist ->
                MediaControllerState.Available.QueueItem.Playlist(
                    id = dbPlaylist.id,
                    name = dbPlaylist.name,
                    image = dbPlaylist.image,
                    items = trackRepo.getPlaylistTracksStatic(dbPlaylist.id).map { dbTrack ->
                        MediaControllerState.Available.QueueItem.Track(
                            id = dbTrack.id,
                            name = dbTrack.name,
                            artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                MediaControllerState.Available.QueueItem.Track.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = dbArtist.image
                                )
                            },
                            album = dbTrack.album_id?.let {
                                albumRepo.getStatic(it)!!.let { dbAlbum ->
                                    MediaControllerState.Available.QueueItem.Track.Album(
                                        id = dbAlbum.id,
                                        name = dbAlbum.name,
                                        image = dbAlbum.image,
                                        releaseDate = dbAlbum.release_date
                                    )
                                }
                            },
                            uri = dbTrack.audio_uri,
                            duration = dbTrack.duration
                        )
                    }
                )
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
            val queue: List<QueueItem>,
            val queueItemIndex: Int,
            val queueSubItemIndex: Int,
            val timePositionChange: StateFlow<Long>,
            val isPlaying: StateFlow<Boolean>,
            val repeatState: RepeatState
        ) : MediaControllerState() {
            val currentTrack: QueueItem.Track get() = when (val result = queue[queueItemIndex]) {
                is QueueItem.Track -> result
                is QueueItem.Playlist -> result.items[queueSubItemIndex]
                is QueueItem.Album -> result.items[queueSubItemIndex]
            }

            enum class RepeatState { Off, List, Track }

            sealed class QueueItem {
                abstract val id: Long

                data class Track(
                    override val id: Long,
                    val name: String,
                    val artists: List<Artist>,
                    val album: Album?,
                    val uri: String?,
                    val duration: Long
                ) : QueueItem() {
                    data class Artist(
                        val id: Long,
                        val name: String,
                        val image: ByteArray?
                    )

                    data class Album(
                        val id: Long,
                        val name: String,
                        val image: ByteArray?,
                        val releaseDate: String?
                    )
                }

                data class Playlist(
                    override val id: Long,
                    val name: String,
                    val image: ByteArray?,
                    val items: List<Track>
                ): QueueItem() {
                    init { if (items.isEmpty()) { TODO() } }
                }

                data class Album(
                    override val id: Long,
                    val name: String,
                    val image: ByteArray?,
                    val releaseDate: String?,
                    val items: List<Track>
                ): QueueItem() {
                    init { if (items.isEmpty()) { TODO() } }
                }
            }
        }
    }

    sealed class QueueItemParameter {
        abstract val id: Long
        data class Track(override val id: Long) : QueueItemParameter()
        data class Playlist(override val id: Long): QueueItemParameter()
        data class Album(override val id: Long): QueueItemParameter()
    }

    abstract class MediaPlayer {
        abstract fun registerEventListener(eventListener: EventListener)
        abstract fun setMedia(uri: String)
        abstract fun play()
        abstract fun pause()
        abstract fun stop()
        abstract fun setTime(time: Long)
        abstract fun release()

        abstract class EventListener {
            abstract fun onPlaying()
            abstract fun onPaused()
            abstract fun onStopped()
            abstract fun onTimePositionChange(time: Long)
            abstract fun onFinished()
        }
    }
}
