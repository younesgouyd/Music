package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class PlaylistList(
    private val playlistRepo: PlaylistRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val folderRepo: FolderRepo,
    private val albumRepo: AlbumRepo,
    private val mediaController: MediaController,
    showPlaylistDetails: (id: Long) -> Unit
) : Component() {
    override val title: String = "Playlists"
    protected val state: MutableStateFlow<PlaylistListState> = MutableStateFlow(PlaylistListState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                PlaylistListState.Loaded(
                    playlists = playlistRepo.getAll().mapLatest { list ->
                        list.map { dbPlaylist ->
                            PlaylistListState.Loaded.PlaylistListItem(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = dbPlaylist.image
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onPlaylist = showPlaylistDetails,
                    onPlayPlaylist = ::playPlaylist,
                    onAddToPlaylist = ::showAddToPlaylistDialog,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onDeletePlaylist = ::deletePlaylist,
                    onRenamePlaylist = ::renamePlaylist,
                    onAddPlaylistToQueue = ::addPlaylistToQueue
                )
            }
        }
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun playPlaylist(id: Long) {
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(id)))
    }

    private fun addPlaylistToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Playlist(id)))
    }

    private fun deletePlaylist(id: Long) {
        coroutineScope.launch {
            playlistRepo.delete(id)
        }
    }

    private fun renamePlaylist(newName: String, id: Long) {
        coroutineScope.launch {
            playlistRepo.updateName(id = id, name = newName)
        }
    }

    private fun showAddToPlaylistDialog(playlistId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Playlist(playlistId),
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

    protected sealed class PlaylistListState {
        data object Loading : PlaylistListState()

        data class Loaded(
            val playlists: StateFlow<List<PlaylistListItem>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onPlaylist: (Long) -> Unit,
            val onPlayPlaylist: (Long) -> Unit,
            val onAddToPlaylist: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit,
            val onDeletePlaylist: (id: Long) -> Unit,
            val onRenamePlaylist: (newName: String, id: Long) -> Unit,
            val onAddPlaylistToQueue: (id: Long) -> Unit
        ) : PlaylistListState() {
            data class PlaylistListItem(
                val id: Long,
                val name: String,
                val image: ByteArray?,
            )
        }
    }
}