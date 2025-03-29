package dev.younesgouyd.apps.music.common.util

abstract class MediaPlayer {
    abstract val status: Status
    abstract val controls: Controls
    abstract val media: Media

    abstract fun release()

    abstract inner class Status {
        abstract val time: Long
    }

    abstract inner class Controls {
        abstract fun play()
        abstract fun pause()
        abstract fun stop()
        abstract fun setTime(time: Long)
    }

    abstract inner class Media {
        abstract val info: Info
        abstract fun start(mrl: String)
        abstract fun startPaused(mrl: String)

        abstract inner class Info {
            abstract val duration: Long
        }
    }
}