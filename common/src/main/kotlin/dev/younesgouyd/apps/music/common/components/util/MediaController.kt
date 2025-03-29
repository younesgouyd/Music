package dev.younesgouyd.apps.music.common.components.util

import dev.younesgouyd.apps.music.common.components.AddToPlaylist
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.MediaPlayer
import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaController(
    private val trackRepo: TrackRepo,
    private val artistRepo: ArtistRepo,
    private val albumRepo: AlbumRepo,
    private val playlistRepo: PlaylistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val folderRepo: FolderRepo,
    private val onAlbumClick: (Long) -> Unit,
    private val onArtistClick: (Long) -> Unit,
    private val mediaPlayer: MediaPlayer,
    private val mediaUtil: MediaUtil
) {
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val mutex = Mutex()
    private val _state: MutableStateFlow<MediaControllerState> = MutableStateFlow(MediaControllerState.Unavailable)
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)
    private val elapsedTime: StateFlow<Long>

    val state: StateFlow<MediaControllerState> get() = _state.asStateFlow()

    init {
        elapsedTime = flow {
            while (true) {
                emit(mediaPlayer.status.time.let { if (it != -1L) it else 0 })
                delay(100)
            }
        }.distinctUntilChanged().stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = 0)
    }

    fun playQueue(queue: List<QueueItemParameter>, queueItemIndex: Int = 0, queueSubItemIndex: Int = 0) {
        require(queue.isNotEmpty())
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> MediaControllerState.Loading
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { mediaPlayer.controls.pause() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
                        }
                    }
                }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> {
                            val newQueue = queue.map { it.toModel() }
                            require(newQueue.isNotEmpty())
                            newQueue[queueItemIndex].let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items[queueSubItemIndex] // TODO: handle invalid subIndex
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items[queueSubItemIndex] // TODO: handle invalid subIndex
                                }
                                if (currentTrack.audio?.url != null) {
                                    mediaPlayer.media.start(currentTrack.audio.url)
                                }
                            }
                            MediaControllerState.Available(
                                enabled = enabled.asStateFlow(),
                                playbackState = MediaControllerState.Available.PlaybackState(
                                    queue = newQueue,
                                    queueItemIndex = queueItemIndex,
                                    queueSubItemIndex = queueSubItemIndex,
                                    isPlaying = true,
                                    repeatState = MediaControllerState.Available.PlaybackState.RepeatState.Off,
                                    duration = mediaPlayer.media.info.duration,
                                    elapsedTime = elapsedTime,
                                    audioOrVideoState = MediaControllerState.Available.PlaybackState.AudioOrVideoState.Audio
                                ),
                                addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                                addToPlaylist = addToPlaylist.asStateFlow(),
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onValueChange = ::seek,
                                onPreviousClick = ::previous,
                                onPlayQueueClick = ::playQueue,
                                onPlayClick = ::play,
                                onPauseClick = ::pause,
                                onNextClick = ::next,
                                onRepeatClick = ::repeat, // TODO
                                onPlayQueueItem = ::playQueueItem,
                                onPlayQueueSubItem = ::playTrackInQueue,
                                onAddToPlaylistClick = ::showAddToPlaylistDialog,
                                onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
                            )
                        }
                        is MediaControllerState.Available -> {
                            if (queue.isEmpty()) {
                                if (!currentState.playbackState.isPlaying) { mediaPlayer.controls.play() }
                                currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = true))
                            } else {
                                val newQueue = queue.map { it.toModel() }
                                val index = queueItemIndex ?: 0
                                val subIndex = queueSubItemIndex ?: 0
                                if (newQueue.isNotEmpty()) {
                                    newQueue[index].let { firstQueueItem ->
                                        val currentTrack = when (firstQueueItem) {
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items[subIndex] // TODO: handle invalid subIndex
                                            is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items[subIndex] // TODO: handle invalid subIndex
                                        }
                                        if (currentTrack.audio?.url != null) {
                                            mediaPlayer.media.start(currentTrack.audio.url)
                                        }
                                    }
                                }
                                currentState.copy(
                                    playbackState = currentState.playbackState.copy(
                                        queue = newQueue,
                                        queueItemIndex = index,
                                        queueSubItemIndex = subIndex,
                                        isPlaying = true,
                                        duration = mediaPlayer.media.info.duration
                                    )
                                )
                            }
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }
    
    fun play() {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (!currentState.playbackState.isPlaying) { mediaPlayer.controls.pause() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = true))
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    fun pause() {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { mediaPlayer.controls.pause() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    fun seek(position: Long) {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                mediaPlayer.controls.setTime(position)
                enabled.update { true }
            }
        }
    }

    fun next() {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                var wasPlaying = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { wasPlaying = true; mediaPlayer.controls.stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
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
                                if (wasPlaying) {
                                    mediaPlayer.media.start(newTrack.audio.url)
                                } else {
                                    mediaPlayer.media.startPaused(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = newIndex,
                                    queueSubItemIndex = newSubIndex,
                                    isPlaying = wasPlaying,
                                    duration = mediaPlayer.media.info.duration
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    fun previous() {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                var wasPlaying = false
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { wasPlaying = true; mediaPlayer.controls.stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
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
                                if (wasPlaying) {
                                    mediaPlayer.media.start(newTrack.audio.url)
                                } else {
                                    mediaPlayer.media.startPaused(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = newIndex,
                                    queueSubItemIndex = newSubIndex,
                                    isPlaying = wasPlaying,
                                    duration = mediaPlayer.media.info.duration
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    fun repeat() {
        coroutineScope.launch {
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
                                        MediaControllerState.Available.PlaybackState.RepeatState.Off -> MediaControllerState.Available.PlaybackState.RepeatState.List
                                        MediaControllerState.Available.PlaybackState.RepeatState.List -> MediaControllerState.Available.PlaybackState.RepeatState.Track
                                        MediaControllerState.Available.PlaybackState.RepeatState.Track -> MediaControllerState.Available.PlaybackState.RepeatState.Off
                                    }
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    fun addToQueue(items: List<QueueItemParameter>) {
        coroutineScope.launch {
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
                            val newQueue = items.map { it.toModel() }
                            newQueue.first().let { firstQueueItem ->
                                val currentTrack = when (firstQueueItem) {
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Track -> firstQueueItem
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Playlist -> firstQueueItem.items.first()
                                    is MediaControllerState.Available.PlaybackState.QueueItem.Album -> firstQueueItem.items.first()
                                }
                                if (currentTrack.audio?.url != null) {
                                    mediaPlayer.media.startPaused(currentTrack.audio.url)
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
                                    duration = mediaPlayer.media.info.duration,
                                    elapsedTime = elapsedTime,
                                    audioOrVideoState = MediaControllerState.Available.PlaybackState.AudioOrVideoState.Audio
                                ),
                                addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                                addToPlaylist = addToPlaylist.asStateFlow(),
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onValueChange = ::seek,
                                onPreviousClick = ::previous,
                                onPlayQueueClick = ::playQueue,
                                onPlayClick = ::play,
                                onPauseClick = ::pause,
                                onNextClick = ::next,
                                onRepeatClick = ::repeat, // TODO
                                onPlayQueueItem = ::playQueueItem,
                                onPlayQueueSubItem = ::playTrackInQueue,
                                onAddToPlaylistClick = ::showAddToPlaylistDialog,
                                onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
                            )
                        }
                        is MediaControllerState.Available -> {
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queue = currentState.playbackState.queue.toMutableList().apply { addAll(items.map { it.toModel() }) }
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    private fun playQueueItem(queueItemIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { mediaPlayer.controls.stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
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
                                    mediaPlayer.media.start(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = queueItemIndex,
                                    queueSubItemIndex = 0,
                                    isPlaying = true,
                                    duration = mediaPlayer.media.info.duration
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    private fun playTrackInQueue(queueItemIndex: Int, trackIndex: Int) {
        coroutineScope.launch {
            mutex.withLock {
                enabled.update { false }
                _state.update { currentState ->
                    when (currentState) {
                        is MediaControllerState.Unavailable -> TODO()
                        is MediaControllerState.Loading -> TODO()
                        is MediaControllerState.Available -> {
                            if (currentState.playbackState.isPlaying) { mediaPlayer.controls.stop() }
                            currentState.copy(playbackState = currentState.playbackState.copy(isPlaying = false))
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
                                    mediaPlayer.media.start(newTrack.audio.url)
                                }
                            }
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    queueItemIndex = queueItemIndex,
                                    queueSubItemIndex = trackIndex,
                                    duration = mediaPlayer.media.info.duration
                                )
                            )
                        }
                    }
                }
                enabled.update { true }
            }
        }
    }

    private suspend fun QueueItemParameter.toModel(): MediaControllerState.Available.PlaybackState.QueueItem {
        return when (this) {
            is QueueItemParameter.Track -> trackRepo.getStatic(this.id)!!.let { dbTrack ->
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
                        albumRepo.getStatic(it)!!.let { dbAlbum ->
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
            is QueueItemParameter.Album -> albumRepo.getStatic(this.id)!!.let { dbAlbum ->
                MediaControllerState.Available.PlaybackState.QueueItem.Album(
                    id = dbAlbum.id,
                    name = dbAlbum.name,
                    image = dbAlbum.image,
                    releaseDate = dbAlbum.release_date,
                    items = trackRepo.getAlbumTracksStatic(dbAlbum.id).map { dbTrack ->
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
            is QueueItemParameter.Playlist -> playlistRepo.getStatic(this.id)!!.let { dbPlaylist ->
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
                                albumRepo.getStatic(it)!!.let { dbAlbum ->
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

    private suspend fun getDuration(mediaPath: String): Long {
        return mediaUtil.getDuration(mediaPath)
    }

    fun release() {
        coroutineScope.cancel()
        mediaPlayer.release()
    }

    private fun showAddToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Track(trackId),
                playlistTrackCrossRefRepo = playlistTrackCrossRefRepo,
                trackRepo = trackRepo,
                albumRepo = albumRepo,
                folderRepo = folderRepo,
                dismiss = ::dismissAddToPlaylistDialog,
                playlistRepo = playlistRepo
            )
        }
        addToPlaylistDialogVisible.update { true }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    sealed class QueueItemParameter {
        abstract val id: Long

        data class Track(
            override val id: Long
        ) : QueueItemParameter()

        data class Playlist(
            override val id: Long,
        ): QueueItemParameter()

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
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onValueChange: (Long) -> Unit,
            val onPreviousClick: () -> Unit,
            val onPlayQueueClick: (queue: List<QueueItemParameter>, queueItemIndex: Int, queueSubItemIndex: Int) -> Unit,
            val onPlayClick: () -> Unit,
            val onPauseClick: () -> Unit,
            val onNextClick: () -> Unit,
            val onRepeatClick: () -> Unit,
            val onPlayQueueItem: (queueItemIndex: Int) -> Unit,
            val onPlayQueueSubItem: (queueItemIndex: Int, trackIndex: Int) ->Unit,
            val onAddToPlaylistClick: (trackId: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : MediaControllerState() {
            data class PlaybackState(
                val queue: List<QueueItem>,
                val queueItemIndex: Int,
                val queueSubItemIndex: Int,
                val isPlaying: Boolean,
                val repeatState: RepeatState,
                val duration: Long,
                val elapsedTime: StateFlow<Long>,
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
                            val duration: Long
                        )

                        data class Video(
                            val url: String,
                            val duration: Long
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

                enum class RepeatState { Off, List, Track }

                enum class AudioOrVideoState { Audio, Video }
            }
        }
    }
}
