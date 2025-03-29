package dev.younesgouyd.apps.music.android

import android.content.Context
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class MediaPlayer(context: Context) : dev.younesgouyd.apps.music.common.util.MediaPlayer() {
    private val libVlc = LibVLC(context)
    private val vlcPlayer = MediaPlayer(libVlc)

    override val status = Status()
    override val controls = Controls()
    override val media = Media()

    override fun release() {
        vlcPlayer.release()
        libVlc.release()
    }

    inner class Status : dev.younesgouyd.apps.music.common.util.MediaPlayer.Status() {
        override val time get() = vlcPlayer.time
    }

    inner class Controls : dev.younesgouyd.apps.music.common.util.MediaPlayer.Controls() {
        override fun play() {
            vlcPlayer.play()
        }

        override fun pause() {
            vlcPlayer.pause()
        }

        override fun stop() {
            vlcPlayer.stop()
        }

        override fun setTime(time: Long) {
            vlcPlayer.setTime(time)
        }
    }

    inner class Media : dev.younesgouyd.apps.music.common.util.MediaPlayer.Media() {
        override val info = Info()

        override fun start(mrl: String) {
            vlcPlayer.media = org.videolan.libvlc.Media(libVlc, mrl)
            vlcPlayer.play()
        }

        override fun startPaused(mrl: String) {
            vlcPlayer.media = org.videolan.libvlc.Media(libVlc, mrl)
            vlcPlayer.pause()
        }

        inner class Info : dev.younesgouyd.apps.music.common.util.MediaPlayer.Media.Info() {
            override val duration: Long get() = vlcPlayer.length
        }
    }
}