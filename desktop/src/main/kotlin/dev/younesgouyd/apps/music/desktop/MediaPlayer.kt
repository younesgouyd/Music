package dev.younesgouyd.apps.music.desktop

import dev.younesgouyd.apps.music.common.util.MediaPlayer
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

class MediaPlayer : MediaPlayer() {
    private val vlcPlayer = AudioPlayerComponent().mediaPlayer()

    override val status = Status()
    override val controls = Controls()
    override val media = Media()

    init {
        NativeDiscovery().discover()
    }

    override fun release() {
        vlcPlayer.release()
    }

    inner class Status : MediaPlayer.Status() {
        override val time get() = vlcPlayer.status().time()
    }

    inner class Controls : MediaPlayer.Controls() {
        override fun play() {
            vlcPlayer.controls().play()
        }

        override fun pause() {
            vlcPlayer.controls().pause()
        }

        override fun stop() {
            vlcPlayer.controls().stop()
        }

        override fun setTime(time: Long) {
            vlcPlayer.controls().setTime(time)
        }
    }

    inner class Media : MediaPlayer.Media() {
        override val info = Info()

        override fun start(mrl: String) {
            vlcPlayer.media().start(mrl)
        }

        override fun startPaused(mrl: String) {
            vlcPlayer.media().startPaused(mrl)
        }

        inner class Info : MediaPlayer.Media.Info() {
            override val duration: Long get() = vlcPlayer.media().info().duration()
        }
    }
}