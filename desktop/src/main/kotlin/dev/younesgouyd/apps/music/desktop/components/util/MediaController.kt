package dev.younesgouyd.apps.music.desktop.components.util

import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

class MediaController : MediaController {
    constructor(
        repoStore: RepoStore
    ) : this(repoStore, MutableStateFlow(false), MutableStateFlow(0))

    private constructor(
        repoStore: RepoStore,
        isPlaying: MutableStateFlow<Boolean>,
        timePositionChange: MutableStateFlow<Long>
    ) : super(MediaPlayer(isPlaying, timePositionChange), repoStore, isPlaying, timePositionChange)

    private class MediaPlayer(
        isPlaying: MutableStateFlow<Boolean>,
        timePositionChange: MutableStateFlow<Long>
    ) : MediaController.MediaPlayer() {
        private val vlcPlayer = AudioPlayerComponent().mediaPlayer()

        init {
            NativeDiscovery().discover()
            vlcPlayer.events().addMediaPlayerEventListener(
                object : MediaPlayerEventAdapter() {
                    override fun playing(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        isPlaying.value = true
                    }

                    override fun paused(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        isPlaying.value = false
                    }

                    override fun stopped(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        isPlaying.value = false
                        timePositionChange.value = 0
                    }

                    override fun timeChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?, newTime: Long) {
                        timePositionChange.value = newTime
                    }
                }
            )
        }

        override fun setMedia(uri: String) { stop(); vlcPlayer.media().startPaused(uri) }
        override fun play() { vlcPlayer.controls().play() }
        override fun pause() { vlcPlayer.controls().pause() }
        override fun stop() { vlcPlayer.controls().stop() }
        override fun setTime(time: Long) { vlcPlayer.controls().setTime(time) }
        override fun release() { vlcPlayer.release() }
    }
}
