package dev.younesgouyd.apps.music.client.multiplatform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import java.io.File
import java.nio.ShortBuffer

actual fun createMediaPlayer(
    clientId: ClientId,
    getFile: suspend (MediaFileId) -> File
): MediaPlayer {
    return MediaPlayerImpl(clientId, getFile)
}

private class MediaPlayerImpl(
    clientId: ClientId,
    getFile: suspend (MediaFileId) -> File
) : LocalMediaPlayer(clientId, getFile) {
    override fun createAudioOutput(): AudioOutput {
        return AudioOutputImpl(
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        )
    }

    private class AudioOutputImpl(
        val out: AudioTrack
    ) : AudioOutput() {
        private var shortArray: ShortArray = ShortArray(0)

        override fun play() {
            out.play()
        }

        override fun write(samples: ShortBuffer) {
            val count = samples.remaining()
            if (shortArray.size < count) {
                shortArray = ShortArray(count)
            }
            samples.get(shortArray, 0, count)
            out.write(shortArray, 0, count, AudioTrack.WRITE_BLOCKING)
            totalShortsWritten += count
        }

        override fun stop() {
            out.stop()
        }

        override fun drain() {
            // NO-OP
        }

        override fun release() {
            out.release()
        }
    }
}