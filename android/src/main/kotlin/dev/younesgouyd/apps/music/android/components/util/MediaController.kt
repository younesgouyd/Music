package dev.younesgouyd.apps.music.android.components.util

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import kotlinx.coroutines.flow.MutableStateFlow

class MediaController : MediaController {
    constructor(
        media3Controller: androidx.media3.session.MediaController,
        repoStore: RepoStore
    ) : this(media3Controller, repoStore, MutableStateFlow(false), MutableStateFlow(0))

    private constructor(
        media3Controller: androidx.media3.session.MediaController,
        repoStore: RepoStore,
        isPlaying: MutableStateFlow<Boolean>,
        timePositionChange: MutableStateFlow<Long>
    ) : super(MediaPlayer(media3Controller, isPlaying, timePositionChange), repoStore, isPlaying, timePositionChange)

    private class MediaPlayer(
        private val media3Controller: androidx.media3.session.MediaController,
        _isPlaying: MutableStateFlow<Boolean>,
        timePositionChange: MutableStateFlow<Long>
    ) : MediaController.MediaPlayer() {
        init {
            media3Controller.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        timePositionChange.value = newPosition.positionMs
                    }
                }
            )
        }

        override fun setMedia(uri: String) {
            media3Controller.setMediaItem(MediaItem.fromUri(uri))
            media3Controller.prepare()
        }

        override fun play() {
            media3Controller.play()
        }

        override fun pause() {
            media3Controller.pause()
        }

        override fun stop() {
            media3Controller.stop()
        }

        override fun setTime(time: Long) {
            media3Controller.seekTo(time)
        }

        override fun release() {
            media3Controller.release()
        }
    }
}
