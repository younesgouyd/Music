package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

abstract class PlaylistDetails(
    private val id: Long,
    private val trackRepo: TrackRepo,
    private val playlistRepo: PlaylistRepo,
    private val artistRepo: ArtistRepo,
    private val albumRepo: AlbumRepo,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val folderRepo: FolderRepo,
    private val mediaController: MediaController,
    showArtistDetails: (id: Long) -> Unit,
    showAlbumDetails: (id: Long) -> Unit
) : Component() {
    override val title: String = "Playlist"
    protected val state: MutableStateFlow<PlaylistDetailsState> = MutableStateFlow(PlaylistDetailsState.Loading)
    private val addToPlaylistDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val addToPlaylist: MutableStateFlow<AddToPlaylist?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            state.update {
                PlaylistDetailsState.Loaded(
                    playlist = playlistRepo.get(id).mapLatest { dbPlaylist ->
                        PlaylistDetailsState.Loaded.Playlist(
                            id = dbPlaylist.id,
                            name = dbPlaylist.name,
                            image = dbPlaylist.image
                        )
                    }.stateIn(coroutineScope),
                    tracks = trackRepo.getPlaylistTracks(id).mapLatest {
                        it.map { dbTrack ->
                            PlaylistDetailsState.Loaded.Track(
                                id = dbTrack.id,
                                name = dbTrack.name,
                                artists = artistRepo.getTrackArtistsStatic(dbTrack.id).map { dbArtist ->
                                    PlaylistDetailsState.Loaded.Track.Artist(
                                        id = dbArtist.id,
                                        name = dbArtist.name
                                    )
                                },
                                album = dbTrack.album_id?.let {
                                    albumRepo.getStatic(it)!!.let { dbAlbum ->
                                        PlaylistDetailsState.Loaded.Track.Album(
                                            id = dbAlbum.id,
                                            name = dbAlbum.name,
                                            image = dbAlbum.image
                                        )
                                    }
                                },
                                addedAt = Instant.ofEpochMilli(dbTrack.added_at).toString()
                            )
                        }
                    }.stateIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList()),
                    addToPlaylistDialogVisible = addToPlaylistDialogVisible.asStateFlow(),
                    addToPlaylist = addToPlaylist.asStateFlow(),
                    onPlayClick = ::play,
                    onAddToQueueClick = ::addToQueue,
                    onTrackClick = ::playTrack,
                    onArtistClick = showArtistDetails,
                    onAlbumClick = showAlbumDetails,
                    onAddToPlaylistClick = ::showAddToPlaylistDialog,
                    onAddTrackToPlaylistClick = ::showAddTrackToPlaylistDialog,
                    onRemoveTrackFromPlaylistClick = ::removeTrackFromPlaylist,
                    onDismissAddToPlaylistDialog = ::dismissAddToPlaylistDialog,
                    onAddTrackToQueueClick = ::addTrackToQueue
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
        mediaController.playQueue(listOf(MediaController.QueueItemParameter.Playlist(id)))
    }

    private fun addToQueue() {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Playlist(id)))
    }

    private fun playTrack(id: Long) {
        coroutineScope.launch {
            val tracks = trackRepo.getPlaylistTracksStatic(this@PlaylistDetails.id)
            val index = tracks.indexOfFirst { it.id == id }
            mediaController.playQueue(
                queue = listOf(MediaController.QueueItemParameter.Playlist(this@PlaylistDetails.id)),
                queueItemIndex = 0,
                queueSubItemIndex = index
            )
        }
    }

    private fun showAddToPlaylistDialog() {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = AddToPlaylist.Item.Playlist(id),
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

    private fun showAddTrackToPlaylistDialog(trackId: Long) {
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

    private fun removeTrackFromPlaylist(trackId: Long) {
        coroutineScope.launch {
            playlistTrackCrossRefRepo.delete(playlistId = id, trackId = trackId)
        }
    }

    private fun dismissAddToPlaylistDialog() {
        if (addToPlaylist.value?.adding?.value == true) {
            return
        }
        addToPlaylistDialogVisible.update { false }
        addToPlaylist.update { it?.clear(); null }
    }

    private fun addTrackToQueue(id: Long) {
        mediaController.addToQueue(listOf(MediaController.QueueItemParameter.Track(id)))
    }

    protected sealed class PlaylistDetailsState {
        data object Loading : PlaylistDetailsState()

        data class Loaded(
            val playlist: StateFlow<Playlist>,
            val tracks: StateFlow<List<Track>>,
            val addToPlaylistDialogVisible: StateFlow<Boolean>,
            val addToPlaylist: StateFlow<Component?>,
            val onPlayClick: () -> Unit,
            val onTrackClick: (id: Long) -> Unit,
            val onArtistClick: (id: Long) -> Unit,
            val onAlbumClick: (id: Long) -> Unit,
            val onAddToPlaylistClick: () -> Unit,
            val onAddToQueueClick: () -> Unit,
            val onAddTrackToPlaylistClick: (id: Long) -> Unit,
            val onRemoveTrackFromPlaylistClick: (id: Long) -> Unit,
            val onDismissAddToPlaylistDialog: () -> Unit,
            val onAddTrackToQueueClick: (id: Long) -> Unit
        ) : PlaylistDetailsState() {
            data class Playlist(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )

            data class Track(
                val id: Long,
                val name: String,
                val artists: List<Artist>,
                val album: Album?,
                val addedAt: String
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
}
