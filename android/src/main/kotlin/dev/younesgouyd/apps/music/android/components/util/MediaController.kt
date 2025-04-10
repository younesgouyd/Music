package dev.younesgouyd.apps.music.android.components.util

import android.content.Context
import android.net.Uri
import dev.younesgouyd.apps.music.android.components.AddToPlaylist
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.util.MediaPlayer
import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import java.io.File

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
    mediaUtil: MediaUtil,
    private val context: Context
) : MediaController(
    trackRepo, artistRepo, albumRepo, playlistRepo, playlistTrackCrossRefRepo,
    folderRepo, onAlbumClick, onArtistClick, mediaPlayer, mediaUtil
) {
    override suspend fun getAudioSource(uri: String): MediaControllerState.Available.PlaybackState.QueueItem.Track.Audio {
        suspend fun saveTempFile(uri: Uri): File {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "temp_${System.currentTimeMillis()}.mp3" // Or guess extension from MIME type if needed
            val file = File(context.cacheDir, fileName)
            inputStream.use { input ->
                file.outputStream().use { output ->
                    input!!.copyTo(output)
                }
            }
            delay(10)
            return file
        }
        val file = saveTempFile(Uri.parse(uri))
        return MediaControllerState.Available.PlaybackState.QueueItem.Track.Audio(
            url = file.absolutePath,
            duration = getDuration(uri)
        )
    }

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

    override fun clearAndroidCache() {
        context.cacheDir.deleteRecursively()
    }
}
