package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mpatric.mp3agic.Mp3File
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Folder
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Playlist
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

abstract class Library(
    private val folderRepo: FolderRepo,
    private val playlistRepo: PlaylistRepo,
    private val trackRepo: TrackRepo,
    protected val albumRepo: AlbumRepo,
    protected val artistRepo: ArtistRepo,
    protected val artistTrackCrossRefRepo: ArtistTrackCrossRefRepo,
    protected val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val mediaController: MediaController
) : Component() {
    override val title: String = "Library"
    protected val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    protected val path: StateFlow<Set<Folder>>
    protected val folders: StateFlow<List<Folder>>
    protected val playlists: StateFlow<List<Playlist>>
    protected val tracks: StateFlow<List<Models.Track>>
    protected val loadingItems: StateFlow<Boolean>
    private val loadingFolders: MutableStateFlow<Boolean>
    private val loadingPlaylists: MutableStateFlow<Boolean>
    private val loadingTracks: MutableStateFlow<Boolean>
    protected val importingFolder: MutableStateFlow<Boolean>
    protected val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    protected val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        loadingFolders = MutableStateFlow(true)
        loadingPlaylists = MutableStateFlow(true)
        loadingTracks = MutableStateFlow(true)
        importingFolder = MutableStateFlow(false)
        loadingItems = combine(loadingFolders, loadingPlaylists, loadingTracks, importingFolder) { loading1, loading2, loading3, loading4 ->
            loading1 || loading2 || loading3 || loading4
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = true)

        path = flow {
            var value: Set<Folder> = emptySet()
            currentFolder.collect { folder ->
                if (folder == null) {
                    value = emptySet()
                    emit(value)
                } else {
                    val list = value.takeWhile { it.id != folder.id }.toMutableList()
                    list.add(folder)
                    value = list.toSet()
                    emit(value)
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptySet())

        folders = currentFolder.flatMapLatest {
            flow {
                loadingFolders.value = true
                folderRepo.getSubfolders(it?.id).collect {
                    emit(it)
                    loadingFolders.value = false
                }
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        playlists = currentFolder.flatMapLatest { currentFolder ->
            flow {
                emit(emptyList())
                loadingPlaylists.value = true
                playlistRepo.getFolderPlaylists(currentFolder?.id).collect { playlist ->
                    emit(playlist)
                    loadingPlaylists.value = false
                }
                loadingPlaylists.value = false
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

        tracks = currentFolder.flatMapLatest { currentFolder ->
            flow {
                loadingTracks.value = true
                if (currentFolder != null) {
                    trackRepo.getFolderTracks(currentFolder.id).collect { tracks ->
                        emit(
                            tracks.map { dbTrack ->
                                Models.Track(
                                    id = dbTrack.id,
                                    name = dbTrack.name,
                                    audioUrl = dbTrack.audio_url,
                                    videoUrl = dbTrack.video_url,
                                    artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                        Models.Track.Artist(
                                            id = dbArtist.id,
                                            name = dbArtist.name
                                        )
                                    },
                                    album = dbTrack.album_id?.let {
                                        albumRepo.getStatic(it)!!.let { dbAlbum ->
                                            Models.Track.Album(
                                                id = dbAlbum.id,
                                                name = dbAlbum.name,
                                                image = dbAlbum.image
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
                loadingTracks.value = false
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    suspend fun saveMp3FileAsTrack(file: File, uri: String, parentFolderId: Long) {
        val mp3file = Mp3File(file)
        var title: String? = null
        var albumTrackNumber: Long? = null
        var artist: String? = null
        var album: String? = null
        var lyrics: String? = null
        var year: String? = null
        var albumImage: ByteArray? = null
        if (mp3file.hasId3v2Tag()) {
            val id3 = mp3file.id3v2Tag
            val albumImageData = id3.albumImage
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
            lyrics = id3.lyrics
            albumImage = albumImageData
        } else if (mp3file.hasId3v1Tag()) {
            val id3 = mp3file.id3v1Tag
            title = id3.title
            albumTrackNumber = id3.track?.toLongOrNull()
            artist = id3.artist
            album = id3.album
            year = id3.year
        }
        var artistId: Long? = null
        var albumId: Long? = null
        if (!artist.isNullOrEmpty()) {
            val artists = artistRepo.getByName(artist)
            if (artists.isEmpty()) {
                artistId = artistRepo.add(name = artist, image = null)
            } else if (artists.size == 1) {
                artistId = artists.first().id
            }
        }
        if (!album.isNullOrEmpty()) {
            val albums = albumRepo.getByName(album)
            if (albums.isEmpty()) {
                albumId = albumRepo.add(name = album, image = albumImage, releaseDate = year)
            } else if (albums.size == 1) {
                albumId = albums.first().id
            }
        }
        val trackId = trackRepo.add(
            name = if (!title.isNullOrEmpty()) title else file.name,
            folderId = parentFolderId,
            albumId = albumId,
            audioUrl = uri,
            videoUrl = null,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            duration = mp3file.lengthInMilliseconds
        )
        if (artistId != null) {
            artistTrackCrossRefRepo.add(artistId, trackId)
        }
    }

    protected fun addTrack(name: String, audioUrl: String?, videoUrl: String?, duration: Long) {
        coroutineScope.launch {
            trackRepo.add(
                name = name,
                folderId = currentFolder.value!!.id,
                albumId = null,
                audioUrl = audioUrl,
                videoUrl = videoUrl,
                lyrics = null,
                albumTrackNumber = null,
                duration = duration
            )
        }
    }

    protected fun playFolder(folderId: Long) {
        suspend fun getFolderItems(_folderId: Long): List<MediaController.QueueItemParameter> {
            val tracks = trackRepo.getFolderTracksStatic(_folderId).map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
            val playlists = playlistRepo.getFolderPlaylistsStatic(_folderId).map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
            return tracks + playlists + folderRepo.getSubfoldersStatic(_folderId).flatMap { getFolderItems(it.id) }
        }
        coroutineScope.launch {
            val queue = getFolderItems(folderId)
            mediaController.playQueue(queue)
        }
    }

    protected abstract fun showAddTrackToPlaylistDialog(trackId: Long)

    protected abstract fun showAddPlaylistToPlaylistDialog(playlistId: Long)

    protected abstract fun showAddFolderToPlaylistDialog(folderId: Long)

    protected fun addFolderToQueue(id: Long) {
        suspend fun getFolderItems(_id: Long): List<MediaController.QueueItemParameter> {
            val tracks = trackRepo.getFolderTracksStatic(_id).map { dbTrack -> MediaController.QueueItemParameter.Track(dbTrack.id) }
            val playlists = playlistRepo.getFolderPlaylistsStatic(_id).map { dbPlaylist -> MediaController.QueueItemParameter.Playlist(dbPlaylist.id) }
            return tracks + playlists + folderRepo.getSubfoldersStatic(_id).flatMap { getFolderItems(it.id) }
        }
        coroutineScope.launch {
            val queue = getFolderItems(id)
            mediaController.addToQueue(queue)
        }
    }

    protected fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    protected object Models {
        data class Track(
            val id: Long,
            val name: String,
            val audioUrl: String?,
            val videoUrl: String?,
            val artists: List<Artist>,
            val album: Album?
        ) {
            data class Artist(
                val id: Long,
                val name: String
            )

            data class Album(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )
        }
    }
}
