package dev.younesgouyd.apps.music.common.components

import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Track
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.GetPlaylistTracks
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AddToPlaylist(
    private val itemToAdd: Item,
    private val playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    private val trackRepo: TrackRepo,
    private val albumRepo: AlbumRepo,
    private val folderRepo: FolderRepo,
    private val dismiss: () -> Unit,
    private val playlistRepo: PlaylistRepo
) : Component() {
    override val title: String = "Add to Playlist"
    protected val state: MutableStateFlow<AddToPlaylistState> = MutableStateFlow(AddToPlaylistState.Loading)
    private val _adding: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val adding: StateFlow<Boolean> get() = _adding.asStateFlow()

    init {
        coroutineScope.launch {
            state.update {
                AddToPlaylistState.Loaded(
                    adding = _adding.asStateFlow(),
                    itemToAdd = when (itemToAdd) {
                        is Item.Track -> trackRepo.getStatic(itemToAdd.id)!!.let { dbTrack ->
                            AddToPlaylistState.Loaded.ItemToAdd.Track(
                                name = dbTrack.name,
                                image = dbTrack.album_id?.let { albumRepo.getStatic(it)!!.image }
                            )
                        }
                        is Item.Album -> albumRepo.getStatic(itemToAdd.id)!!.let { dbAlbum ->
                            AddToPlaylistState.Loaded.ItemToAdd.Album(
                                name = dbAlbum.name,
                                image = dbAlbum.image
                            )
                        }
                        is Item.Playlist -> playlistRepo.getStatic(itemToAdd.id)!!.let { dbPlaylist ->
                            AddToPlaylistState.Loaded.ItemToAdd.Playlist(
                                name = dbPlaylist.name,
                                image = dbPlaylist.image
                            )
                        }
                        is Item.Folder -> folderRepo.getStatic(itemToAdd.id)!!.let { dbFolder ->
                            AddToPlaylistState.Loaded.ItemToAdd.Folder(
                                name = dbFolder.name
                            )
                        }
                    },
                    playlists = playlistRepo.getAll().map { list ->
                        list.map { dbPlaylist ->
                            AddToPlaylistState.Loaded.PlaylistOption(
                                id = dbPlaylist.id,
                                name = dbPlaylist.name,
                                image = dbPlaylist.image
                            )
                        }
                    }.stateIn(coroutineScope),
                    onAddTopPlaylist = ::addToPlaylist
                )
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun addToPlaylist(playlistToAddTo: AddToPlaylistState.Loaded.PlaylistToAddTo) {
        coroutineScope.launch {
            _adding.update { true }
            val playlistId = when (playlistToAddTo) {
                is AddToPlaylistState.Loaded.PlaylistToAddTo.Id -> playlistToAddTo.value
                is AddToPlaylistState.Loaded.PlaylistToAddTo.New -> playlistRepo.add(name = playlistToAddTo.name, folderId = null, image = null)
            }
            when (itemToAdd) {
                is Item.Track -> {
                    val exists = playlistTrackCrossRefRepo.getStatic(playlistId, itemToAdd.id) != null
                    if (!exists) {
                        playlistTrackCrossRefRepo.add(playlistId, itemToAdd.id)
                    }
                }
                is Item.Album -> {
                    val tracks: List<Track> = trackRepo.getAlbumTracksStatic(itemToAdd.id)
                    for (track in tracks) {
                        val exists = playlistTrackCrossRefRepo.getStatic(playlistId, track.id) != null
                        if (!exists) {
                            playlistTrackCrossRefRepo.add(playlistId, track.id)
                        }
                    }
                }
                is Item.Playlist -> {
                    val tracks: List<GetPlaylistTracks> = trackRepo.getPlaylistTracksStatic(itemToAdd.id)
                    for (track in tracks) {
                        val exists = playlistTrackCrossRefRepo.getStatic(playlistId, track.id) != null
                        if (!exists) {
                            playlistTrackCrossRefRepo.add(playlistId, track.id)
                        }
                    }
                }
                is Item.Folder -> {
                    suspend fun addFolderToPlaylist(folderId: Long) {
                        val tracks: List<Track> = trackRepo.getFolderTracksStatic(folderId)
                        for (track in tracks) {
                            val exists = playlistTrackCrossRefRepo.getStatic(playlistId, track.id) != null
                            if (!exists) {
                                playlistTrackCrossRefRepo.add(playlistId, track.id)
                            }
                        }
                        val subfolders = folderRepo.getSubfoldersStatic(folderId)
                        for (subfolder in subfolders) {
                            addFolderToPlaylist(subfolder.id)
                        }
                    }
                    addFolderToPlaylist(itemToAdd.id)
                }
            }
            _adding.update { false }
            dismiss()
        }
    }

    sealed class Item {
        abstract val id: Long

        data class Track(override val id: Long) : Item()

        data class Album(override val id: Long) : Item()

        data class Playlist(override val id: Long) : Item()

        data class Folder(override val id: Long) : Item()
    }

    protected sealed class AddToPlaylistState {
        data object Loading : AddToPlaylistState()

        data class Loaded(
            val adding: StateFlow<Boolean>,
            val itemToAdd: ItemToAdd,
            val playlists: StateFlow<List<PlaylistOption>>,
            val onAddTopPlaylist: (playlistId: PlaylistToAddTo) -> Unit
        ) : AddToPlaylistState() {
            sealed class ItemToAdd {
                abstract val name: String
                abstract val image: ByteArray?

                data class Track(
                    override val name: String,
                    override val image: ByteArray?
                ) : ItemToAdd()

                data class Album(
                    override val name: String,
                    override val image: ByteArray?
                ) : ItemToAdd()

                data class Playlist(
                    override val name: String,
                    override val image: ByteArray?
                ) : ItemToAdd()

                data class Folder(
                    override val name: String
                ) : ItemToAdd() {
                    override val image: ByteArray? = null
                }
            }

            data class PlaylistOption(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )

            sealed class PlaylistToAddTo {
                data class Id(val value: Long) : PlaylistToAddTo()

                data class New(val name: String) : PlaylistToAddTo()
            }
        }
    }
}