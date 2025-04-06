package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.AlbumDetails.AlbumDetailsState.Loaded.Album
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
abstract class AlbumDetails(
    protected val id: Long,
    protected val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    protected val trackRepo: TrackRepo,
    protected val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    protected val playlistRepo: PlaylistRepo,
    protected val folderRepo: FolderRepo,
    private val mediaController: MediaController,
    private val showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Album"
    protected val state: MutableStateFlow<AlbumDetailsState> = MutableStateFlow(AlbumDetailsState.Loading)
    protected val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    protected val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                AlbumDetailsState.Loaded(
                    album = albumRepo.get(id).mapLatest { dbAlbum ->
                        Album(
                            id = dbAlbum.id,
                            name = dbAlbum.name,
                            artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                Album.Artist(
                                    id = dbArtist.id,
                                    name = dbArtist.name
                                )
                            },
                            image = dbAlbum.image,
                            releaseDate = dbAlbum.release_date
                        )
                    }.stateIn(coroutineScope),
                    tracks = trackRepo.getAlbumTracks(id).mapLatest { list ->
                        list.map { dbTrack ->
                            Album.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    Album.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onArtistClick = showArtistDetails,
                    onPlayClick = ::play,
                    onAddToQueueClick = ::addToQueue,
                    onTrackClick = ::playTrack,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onAddTrackToQueue = ::addTrackToQueue,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
                )
            }
        }
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun play() {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun addToQueue() {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun playTrack(id: Long) {
        coroutineScope.launch {
            val tracks = trackRepo.getAlbumTracksStatic(this@AlbumDetails.id)
            val index = tracks.indexOfFirst { it.id == id }
            mediaController.playQueue(
                queue = listOf(MediaController.QueueItemParameter.Album(this@AlbumDetails.id)),
                queueItemIndex = 0,
                queueSubItemIndex = index
            )
        }
    }

    protected abstract fun showAddToPlaylistDialog()

    private fun addTrackToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(id)))
    }

    protected abstract fun showAddTrackToPlaylistDialog(trackId: Long)

    protected fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    protected sealed class AlbumDetailsState {
        data object Loading : AlbumDetailsState()

        data class Loaded(
            val album: StateFlow<Album>,
            val tracks: StateFlow<List<Album.Track>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onArtistClick: (id: Long) -> Unit,
            val onPlayClick: () -> Unit,
            val onAddToQueueClick: () -> Unit,
            val onTrackClick: (id: Long) -> Unit,
            val onAddToPlaylistClick: () -> Unit,
            val onAddTrackToPlaylistClick: (id: Long) -> Unit,
            val onAddTrackToQueue: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : AlbumDetailsState() {
            data class Album(
                val id: Long,
                val name: String,
                val artists: List<Artist>,
                val image: ByteArray?,
                val releaseDate: String?,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )

                data class Track(
                    val id: Long,
                    val name: String,
                    val artists: List<Artist>
                ) {
                    data class Artist(
                        val id: Long,
                        val name: String
                    )
                }
            }

        }
    }
}