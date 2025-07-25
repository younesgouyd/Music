package dev.younesgouyd.apps.music.common.components

import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ArtistDetails(
    private val id: Long,
    private val artistRepo: ArtistRepo,
    protected val albumRepo: AlbumRepo,
    protected val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    protected val trackRepo: TrackRepo,
    protected val folderRepo: FolderRepo,
    protected val playlistRepo: PlaylistRepo,
    private val mediaController: MediaController,
    private val showAlbumDetails: (Long) -> Unit,
    private val showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Artist"
    protected val state: MutableStateFlow<ArtistDetailsState> = MutableStateFlow(ArtistDetailsState.Loading)
    protected val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    protected val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                ArtistDetailsState.Loaded(
                    artist = artistRepo.get(id).mapLatest { dbArtist ->
                        ArtistDetailsState.Loaded.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name,
                            image = dbArtist.image
                        )
                    }.stateIn(coroutineScope),
                    albums = albumRepo.getArtistAlbums(id).mapLatest { list ->
                        list.map { dbAlbum ->
                            ArtistDetailsState.Loaded.Artist.Album(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date,
                                artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                    ArtistDetailsState.Loaded.Artist.Album.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(coroutineScope),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onAlbumClick = showAlbumDetails,
                    onArtistClick = showArtistDetails,
                    onPlayAlbumClick = ::playAlbum,
                    onAddAlbumToPlaylistClick = ::showAddAlbumToPlaylistDialog,
                    onAddAlbumToQueueClick = ::addAlbumToQueueClick,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog
                )
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun playAlbum(id: Long) {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    protected abstract fun showAddAlbumToPlaylistDialog(albumId: Long)

    private fun addAlbumToQueueClick(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    protected fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    protected sealed class ArtistDetailsState {
        data object Loading : ArtistDetailsState()

        data class Loaded(
            val artist: StateFlow<Artist>,
            val albums: StateFlow<List<Artist.Album>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onPlayAlbumClick: (Long) -> Unit,
            val onAddAlbumToPlaylistClick: (id: Long) -> Unit,
            val onAddAlbumToQueueClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : ArtistDetailsState() {
            data class Artist(
                val id: Long,
                val name: String,
                val image: ByteArray?
            ) {
                data class Track(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?
                )

                data class Album(
                    val id: Long,
                    val name: String,
                    val image: ByteArray?,
                    val releaseDate: String?,
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
