package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AlbumList(
    private val albumRepo: AlbumRepo,
    private val artistRepo: ArtistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val folderRepo: FolderRepo,
    private val mediaController: MediaController,
    showAlbumDetails: (Long) -> Unit,
    showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Albums"
    protected val state: MutableStateFlow<AlbumListState> = MutableStateFlow(AlbumListState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                AlbumListState.Loaded(
                    albums = albumRepo.getAll().mapLatest { list ->
                        list.map { dbAlbum ->
                            AlbumListState.Loaded.AlbumListItem(
                                id = dbAlbum.id,
                                name = dbAlbum.name,
                                image = dbAlbum.image,
                                releaseDate = dbAlbum.release_date,
                                artists = artistRepo.getAlbumArtistsStatic(dbAlbum.id).map { dbArtist ->
                                    AlbumListState.Loaded.AlbumListItem.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                }
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onAlbumClick = showAlbumDetails,
                    onArtistClick = showArtistDetails,
                    onPlayAlbumClick = ::playAlbum,
                    onAddAlbumToQueueClick = ::addAlbumToQueue,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
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

    private fun playAlbum(id: Long) {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun addAlbumToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Album(id)))
    }

    private fun showAddToPlaylistDialog(albumId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Album(albumId),
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

    protected sealed class AlbumListState {
        data object Loading : AlbumListState()

        data class Loaded(
            val albums: StateFlow<List<AlbumListItem>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onAlbumClick: (Long) -> Unit,
            val onArtistClick: (Long) -> Unit,
            val onPlayAlbumClick: (Long) -> Unit,
            val onAddAlbumToQueueClick: (Long) -> Unit,
            val onAddToPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit
        ) : AlbumListState() {
            data class AlbumListItem(
                val id: Long,
                val name: String,
                val image: ByteArray?,
                val releaseDate: String?,
                val artists: List<Artist>,
            ) {
                data class Artist(
                    val id: Long,
                    val name: String
                )
            }
        }
    }
}