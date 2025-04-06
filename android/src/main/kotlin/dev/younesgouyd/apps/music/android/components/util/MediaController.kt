package dev.younesgouyd.apps.music.android.components.util

import dev.younesgouyd.apps.music.android.components.AddToPlaylist
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.MediaPlayer
import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.flow.update

class MediaController(
    trackRepo: TrackRepo,
    artistRepo: ArtistRepo,
    albumRepo: AlbumRepo,
    playlistRepo: PlaylistRepo,
    playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo,
    folderRepo: FolderRepo,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    mediaPlayer: MediaPlayer,
    mediaUtil: MediaUtil
) : MediaController(
    trackRepo, artistRepo, albumRepo, playlistRepo, playlistTrackCrossRefRepo,
    folderRepo, onAlbumClick, onArtistClick, mediaPlayer, mediaUtil
) {
    override fun showAddToPlaylistDialog(trackId: Long) {
        addToPlaylist.update {
            AddToPlaylist(
                itemToAdd = dev.younesgouyd.apps.music.common.components.AddToPlaylist.Item.Track(trackId),
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
}
