package dev.younesgouyd.apps.music.common

import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackContext
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.net.URI

abstract class MediaPlayer {
    abstract val state: StateFlow<MediaPlayerState>
    abstract suspend fun setTrack(id: MediaFileId)
    abstract suspend fun play(context: PlaybackContext.ServerSent)
    abstract suspend fun pause()
    abstract suspend fun stop()
    abstract suspend fun setPositionMs(positionMs: Long, context: PlaybackContext.ServerSent)
    abstract suspend fun setState(value: MediaPlayerState, context: PlaybackContext.ServerSent?)
    abstract suspend fun release()

    protected fun File.resolved(): String {
        val uri = this.toPath().toUri().toString()
        val resolved = if (uri.startsWith("file:")) {
            File(URI(uri)).absolutePath
        } else uri
        return resolved
    }
}