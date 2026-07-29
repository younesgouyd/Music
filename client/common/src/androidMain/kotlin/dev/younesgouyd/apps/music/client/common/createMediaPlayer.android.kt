package dev.younesgouyd.apps.music.client.common

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.SessionToken
import dev.younesgouyd.apps.music.common.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual suspend fun createMediaPlayer(): MediaController.MediaPlayer {
    return withContext(Dispatchers.IO) {
        val sessionToken = SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java))
        val media3Controller = androidx.media3.session.MediaController.Builder(applicationContext, sessionToken)
            .buildAsync()
            .get()
        val handler = Handler(Looper.getMainLooper())
        object : MediaController.MediaPlayer() {
            override fun registerEventListener(eventListener: EventListener) {
                val updateRunnable = object : Runnable {
                    override fun run() {
                        if (media3Controller.isPlaying) {
                            eventListener.onTimePositionChange(
                                media3Controller.currentPosition.milliseconds
                            )
                        }
                        handler.postDelayed(this, 250)
                    }
                }

                media3Controller.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                eventListener.onPlaying()
                                handler.post(updateRunnable)
                            } else {
                                eventListener.onPaused()
                                handler.removeCallbacks(updateRunnable)
                            }
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                handler.removeCallbacks(updateRunnable)
                                eventListener.onFinished()
                            }
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

            override fun setTime(time: Duration) {
                media3Controller.seekTo(time.inWholeMilliseconds)
            }

            override fun release() {
                media3Controller.release()
                handler.removeCallbacksAndMessages(null)
            }
        }
    }
}