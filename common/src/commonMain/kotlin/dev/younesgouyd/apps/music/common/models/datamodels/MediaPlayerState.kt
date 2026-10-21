package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class MediaPlayerState(
    val track: MediaFileId?,
    val isPlaying: Boolean,
    val startPositionMs: Long,
    val isFinished: Boolean,
    private val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        fun initial() = MediaPlayerState(null, false, 0L, false)
    }

    // TODO (may become greater than duration)
    val positionMs: Long get() {
        return if (isPlaying) {
            startPositionMs + (Clock.System.now().toEpochMilliseconds() - timestamp).coerceAtLeast(0L)
        } else {
            startPositionMs
        }
    }
}