package dev.younesgouyd.apps.music.app.components.util.widgets

import dev.younesgouyd.apps.music.app.data.repoes.AlbumRepo
import dev.younesgouyd.apps.music.app.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.app.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.app.data.repoes.TrackRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MediaController(
    private val coroutineScope: CoroutineScope,
    private val trackRepo: TrackRepo,
    private val artistRepo: ArtistRepo,
    private val albumRepo: AlbumRepo,
    private val playlistRepo: PlaylistRepo,
    private val onAlbumClick: (Long) -> Unit,
    private val onArtistClick: (Long) -> Unit
) {
    private val mutex = Mutex()
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val vlcPlayer = AudioPlayerComponent().mediaPlayer()

    val state: StateFlow<MediaControllerState> get() = _state.asStateFlow()

    init {
        NativeDiscovery().discover()
    }

    suspend fun play(queue: List<QueueItemParameter>) {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> {
                        if (queue.isEmpty()) { TODO() }
                        MediaControllerState.Loading
                    }
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().pause() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> {
                        val newQueue = queue.map { it.toModel() }
                        newQueue.first().let { firstQueueItem ->
                            val currentTrack = when (firstQueueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                            }
                            if (currentTrack.audio?.url != null) {
                                vlcPlayer.media().start(currentTrack.audio.url)
                            }
                        }
                        MediaControllerState.Available(
                            enabled = enabled.asStateFlow(),
                            playbackState = MediaControllerState.Available.PlaybackState(
                                queue = newQueue,
                                queueItemIndex = 0,
                                queueSubItemIndex = 0,
                                isPlaying = true,
                                repeatState = MediaControllerState.Available.PlaybackState.RepeatState.Off,
                                duration = vlcPlayer.media().info().duration().milliseconds,
                                elapsedTime = Duration.ZERO,
                                audioOrVideoState = MediaControllerState.Available.PlaybackState.AudioOrVideoState.Audio
                            ),
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onValueChange = { coroutineScope.launch { seek(it) } },
                            onPreviousClick = { coroutineScope.launch { previous() } },
                            onPlayClick = { coroutineScope.launch { play(it) } },
                            onPauseClick = { coroutineScope.launch { pause() } },
                            onNextClick = { coroutineScope.launch { next() } },
                            onCompleted = { coroutineScope.launch { next() } }, // todo
                            onRepeatClick = { coroutineScope.launch { repeat() } }, // TODO
                            onPlayQueueItem = { coroutineScope.launch { playQueueItem(it) } },
                            onPlayQueueSubItem = { queueItemIndex: Int, trackIndex: Int ->
                                coroutineScope.launch { playTrackInQueue(queueItemIndex, trackIndex) }
                            }
                        )
                    }
                    is MediaControllerState.Available -> {
                        if (queue.isEmpty()) {
                            if (!currentState.playbackState.isPlaying) { vlcPlayer.controls().play() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = true))
                        } else {
                            val newQueue = queue.map { it.toModel() }
                            newQueue.first().let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                                }
                                if (currentTrack.audio?.url != null) {
                                    vlcPlayer.media().start(currentTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queue = newQueue,
                                    queueItemIndex = 0,
                                    queueSubItemIndex = 0,
                                    isPlaying = true,
                                    duration = vlcPlayer.media().info().duration().milliseconds
                                )
                            )
                        }
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun pause() {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().pause() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun seek(position: Duration) {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        vlcPlayer.controls().setTime(position.inWholeMilliseconds)
                        currentState.copy(playbackState = currentState.playbackState.copy(elapsedTime = position))
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun next() {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        val queue = currentState.playbackState.queue
                        val currentIndex = currentState.playbackState.queueItemIndex
                        val currentSubIndex = currentState.playbackState.queueSubItemIndex
                        val newIndex: Int
                        val newSubIndex: Int
                        queue[currentIndex].let { queueItem ->
                            when (queueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> {
                                    newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                    newSubIndex = 0
                                }
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> {
                                    if (currentSubIndex + 1 > queueItem.items.size - 1) {
                                        newIndex = if (currentIndex + 1 > queue.size - 1) 0 else currentIndex + 1
                                        newSubIndex = 0
                                    } else {
                                        newIndex = currentIndex
                                        newSubIndex = currentSubIndex + 1
                                    }
                                }
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> {
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
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[newSubIndex]
                            }
                        }
                        if (newTrack.audio?.url != null) {
                            vlcPlayer.media().start(newTrack.audio.url)
                        }
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                queueItemIndex = newIndex,
                                queueSubItemIndex = newSubIndex,
                                isPlaying = true,
                                duration = vlcPlayer.media().info().duration().milliseconds
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun previous() {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        val queue = currentState.playbackState.queue
                        val currentIndex = currentState.playbackState.queueItemIndex
                        val currentSubIndex = currentState.playbackState.queueSubItemIndex
                        val newIndex: Int
                        val newSubIndex: Int
                        queue[currentIndex].let { queueItem ->
                            when (queueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> {
                                    newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                    newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                        when (it) {
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Track -> 0
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
                                        }
                                    }
                                }
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> {
                                    if (currentSubIndex - 1 < 0) {
                                        newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                        newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                            when (it) {
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> 0
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
                                            }
                                        }
                                    } else {
                                        newIndex = currentIndex
                                        newSubIndex = currentSubIndex - 1
                                    }
                                }
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> {
                                    if (currentSubIndex - 1 < 0) {
                                        newIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                                        newSubIndex = if (currentIndex - 1 < 0) 0 else queue[currentIndex - 1].let {
                                            when (it) {
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> 0
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> it.items.lastIndex
                                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> it.items.lastIndex
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
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[newSubIndex]
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[newSubIndex]
                            }
                        }
                        if (newTrack.audio?.url != null) {
                            vlcPlayer.media().start(newTrack.audio.url)
                        }
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                queueItemIndex = newIndex,
                                queueSubItemIndex = newSubIndex,
                                isPlaying = true,
                                duration = vlcPlayer.media().info().duration().milliseconds
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun repeat() {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                repeatState = when (currentState.playbackState.repeatState) {
                                    MediaControllerState.Available.PlaybackState.RepeatState.Off -> MediaControllerState.Available.PlaybackState.RepeatState.Track
                                    MediaControllerState.Available.PlaybackState.RepeatState.Track -> MediaControllerState.Available.PlaybackState.RepeatState.List
                                    MediaControllerState.Available.PlaybackState.RepeatState.List -> MediaControllerState.Available.PlaybackState.RepeatState.Off
                                }
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    suspend fun addToQueue(item: QueueItemParameter) {
        mutex.withLock {
            enabled.update { false }
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
                        val newQueue = listOf(item.toModel())
                        newQueue.first().let { firstQueueItem ->
                            val currentTrack = when (firstQueueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                            }
                            if (currentTrack.audio?.url != null) {
                                vlcPlayer.media().startPaused(currentTrack.audio.url)
                            }
                        }
                        MediaControllerState.Available(
                            enabled = enabled.asStateFlow(),
                            playbackState = MediaControllerState.Available.PlaybackState(
                                queue = newQueue,
                                queueItemIndex = 0,
                                queueSubItemIndex = 0,
                                isPlaying = false,
                                repeatState = MediaControllerState.Available.PlaybackState.RepeatState.Off,
                                duration = vlcPlayer.media().info().duration().milliseconds,
                                elapsedTime = Duration.ZERO,
                                audioOrVideoState = MediaControllerState.Available.PlaybackState.AudioOrVideoState.Audio
                            ),
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onValueChange = { coroutineScope.launch { seek(it) } },
                            onPreviousClick = { coroutineScope.launch { previous() } },
                            onPlayClick = { coroutineScope.launch { play(it) } },
                            onPauseClick = { coroutineScope.launch { pause() } },
                            onNextClick = { coroutineScope.launch { next() } },
                            onCompleted = { coroutineScope.launch { next() } }, // todo
                            onRepeatClick = { coroutineScope.launch { repeat() } }, // TODO
                            onPlayQueueItem = { coroutineScope.launch { playQueueItem(it) } },
                            onPlayQueueSubItem = { queueItemIndex: Int, trackIndex: Int ->
                                coroutineScope.launch { playTrackInQueue(queueItemIndex, trackIndex) }
                            }
                        )
                    }
                    is MediaControllerState.Available -> {
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                queue = currentState.playbackState.queue.toMutableList().apply { add(item.toModel()) }
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    private suspend fun playQueueItem(queueItemIndex: Int) {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        currentState.playbackState.queue[queueItemIndex].let { queueItem ->
                            val newTrack = when (queueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items.first()
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> queueItem.items.first()
                            }
                            if (newTrack.audio?.url != null) {
                                vlcPlayer.media().start(newTrack.audio.url)
                            }
                        }
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                queueItemIndex = queueItemIndex,
                                queueSubItemIndex = 0,
                                isPlaying = true,
                                duration = vlcPlayer.media().info().duration().milliseconds,
                                elapsedTime = Duration.ZERO
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    private suspend fun playTrackInQueue(queueItemIndex: Int, trackIndex: Int) {
        mutex.withLock {
            enabled.update { false }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        if (currentState.playbackState.isPlaying) { vlcPlayer.controls().stop() }
                        currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false, elapsedTime = Duration.ZERO))
                    }
                }
            }
            _state.update { currentState ->
                when (currentState) {
                    is MediaControllerState.Unavailable -> TODO()
                    is MediaControllerState.Loading -> TODO()
                    is MediaControllerState.Available -> {
                        currentState.playbackState.queue[queueItemIndex].let { queueItem ->
                            val newTrack = when (queueItem) {
                                is MediaControllerState.Available.PlaybackState.QueueItem.Track -> queueItem
                                is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> queueItem.items[trackIndex]
                                is MediaControllerState.Available.PlaybackState.QueueItem.Album -> queueItem.items[trackIndex]
                            }
                            if (newTrack.audio?.url != null) {
                                vlcPlayer.media().start(newTrack.audio.url)
                            }
                        }
                        currentState.copy(
                            playbackState = currentState.playbackState.copy(
                                queueItemIndex = queueItemIndex,
                                queueSubItemIndex = trackIndex,
                                duration = vlcPlayer.media().info().duration().milliseconds,
                                elapsedTime = Duration.ZERO
                            )
                        )
                    }
                }
            }
            enabled.update { true }
        }
    }

    private suspend fun QueueItemParameter.toModel(): MediaControllerState.Available.PlaybackState.QueueItem {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.getStatic(this.id).let { dbTrack ->
                MediaControllerState.Available.PlaybackState.QueueItem.Track(
                    id = dbTrack.id,
                    name = dbTrack.name,
                    artists = artistRepo.getTrackArtistsStatic(this.id)
                        .map { dbArtist ->
                            MediaControllerState.Available.PlaybackState.QueueItem.Track.Artist(
                                id = dbArtist.id,
                                name = dbArtist.name,
                                image = dbArtist.image
                            )
                        },
                    album = dbTrack.album_id?.let {
                        albumRepo.getStatic(it).let { dbAlbum ->
                            MediaControllerState.Available.PlaybackState.QueueItem.Track.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date
                            )
                        }
                    },
                    audio = dbTrack.audio_url?.let {
                        MediaControllerState.Available.PlaybackState.QueueItem.Track.Audio(
                            url = it.replaceFirst("file:/", "file:///"),
                            duration = getDuration(it)
                        )
                    },
                    video = dbTrack.video_url?.let {
                        MediaControllerState.Available.PlaybackState.QueueItem.Track.Video(
                            url = it.replaceFirst("file:/", "file:///"),
                            duration = getDuration(it)
                        )
                    }
                )
            }
            is QueueItemParameter.Album -> albumRepo.getStatic(this.id).let { dbAlbum ->
                MediaControllerState.Available.PlaybackState.QueueItem.Album(
                    id = dbAlbum.id,
                    name = dbAlbum.name,
                    image = dbAlbum.image,
                    releaseDate = dbAlbum.release_date,
                    items = trackRepo.getArtistTracksStatic(dbAlbum.id).map { dbTrack ->
                        MediaControllerState.Available.PlaybackState.QueueItem.Track(
                            id = dbTrack.id,
                            name = dbTrack.name,
                            artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = dbArtist.image
                                )
                            },
                            album = MediaControllerState.Available.PlaybackState.QueueItem.Track.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date
                            ),
                            audio = dbTrack.audio_url?.let {
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Audio(
                                    url = it.replaceFirst("file:/", "file:///"),
                                    duration = getDuration(it)
                                )
                            },
                            video = dbTrack.video_url?.let {
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Video(
                                    url = it.replaceFirst("file:/", "file:///"),
                                    duration = getDuration(it)
                                )
                            }
                        )
                    }
                )
            }
            is QueueItemParameter.Playlist -> playlistRepo.getStatic(this.id).let { dbPlaylist ->
                MediaControllerState.Available.PlaybackState.QueueItem.Playlist(
                    id = dbPlaylist.id,
                    name = dbPlaylist.name,
                    image = dbPlaylist.image,
                    items = trackRepo.getPlaylistTracksStatic(dbPlaylist.id).map { dbTrack ->
                        MediaControllerState.Available.PlaybackState.QueueItem.Track(
                            id = dbTrack.id,
                            name = dbTrack.name,
                            artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name,
                                    image = dbArtist.image
                                )
                            },
                            album = dbTrack.album_id?.let {
                                albumRepo.getStatic(it).let { dbAlbum ->
                                    MediaControllerState.Available.PlaybackState.QueueItem.Track.Album(
                                        id = dbAlbum.id,
                                        name = dbAlbum.name,
                                        image = dbAlbum.image,
                                        releaseDate = dbAlbum.release_date
                                    )
                                }
                            },
                            audio = dbTrack.audio_url?.let {
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Audio(
                                    url = it.replaceFirst("file:/", "file:///"),
                                    duration = getDuration(it)
                                )
                            },
                            video = dbTrack.video_url?.let {
                                MediaControllerState.Available.PlaybackState.QueueItem.Track.Video(
                                    url = it.replaceFirst("file:/", "file:///"),
                                    duration = getDuration(it)
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    private suspend fun getDuration(mediaPath: String): Duration {
        return withContext(Dispatchers.IO) {
            val factory = MediaPlayerFactory()
            val media = factory.media().newMedia(mediaPath)
            try {
                if (media.parsing().parse()) {
                    media.info().duration().milliseconds
                } else {
                    TODO()
                }
            } finally {
                media.release()
                factory.release()
            }
        }
    }

    fun release() {
        vlcPlayer.release()
    }

    sealed class QueueItemParameter {
        abstract val id: Long

        data class Track(
            override val id: Long
        ) : QueueItemParameter()

        data class Playlist(
            override val id: Long,
            val orderBy: OrderBy
        ): QueueItemParameter() {
            data class OrderBy(
                val order: Order,
                val asc: Boolean
            ) {
                enum class Order { AddedDate, Custom }
            }
        }

        data class Album(
            override val id: Long
        ): QueueItemParameter()
    }

    sealed class MediaControllerState {
        data object Loading : MediaControllerState()

        data object Unavailable : MediaControllerState()

        data class Available(
            val enabled: StateFlow<Boolean>,
            val playbackState: PlaybackState,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onValueChange: (Duration) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayClick: (queue: List<QueueItemParameter>) -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onCompleted: () -> Unit,
            val onRepeatClick: (PlaybackState.RepeatState) -> Unit,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) ->Unit
        ) : MediaControllerState() {
            data class PlaybackState(
                val queue: List<QueueItem>,
                val queueItemIndex: Int,
                val queueSubItemIndex: Int,
                val isPlaying: Boolean,
                val repeatState: RepeatState,
                val duration: Duration,
                val elapsedTime: Duration,
                val audioOrVideoState: AudioOrVideoState
            ) {
                val currentTrack get() = when (val it = queue[queueItemIndex]) {
                    is QueueItem.Track -> it
                    is QueueItem.Playlist -> it.items[queueSubItemIndex]
                    is QueueItem.Album -> it.items[queueSubItemIndex]
                }

                init { if (queue.isEmpty()) { TODO() } }

                sealed class QueueItem {
                    data class Track(
                        val id: Long,
                        val name: String,
                        val artists: List<Artist>,
                        val album: Album?,
                        val audio: Audio?,
                        val video: Video?
                    ) : QueueItem() {
                        val playable = audio?.url != null

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

                        data class Audio(
                            val url: String,
                            val duration: Duration
                        )

                        data class Video(
                            val url: String,
                            val duration: Duration
                        )
                    }

                    data class Playlist(
                        val id: Long,
                        val name: String,
                        val image: ByteArray?,
                        val items: List<Track>
                    ): QueueItem() {
                        init { if (items.isEmpty()) { TODO() } }
                    }

                    data class Album(
                        val id: Long,
                        val name: String,
                        val image: ByteArray?,
                        val releaseDate: String?,
                        val items: List<Track>
                    ): QueueItem() {
                        init { if (items.isEmpty()) { TODO() } }
                    }
                }

                enum class RepeatState { Off, Track, List }

                enum class AudioOrVideoState { Audio, Video }
            }
        }
    }
}
