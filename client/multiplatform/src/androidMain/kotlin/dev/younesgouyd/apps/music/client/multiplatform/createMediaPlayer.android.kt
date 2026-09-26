package dev.younesgouyd.apps.music.client.multiplatform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.MediaFileId
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ShortBuffer

actual fun createMediaPlayer(
    getFile: suspend (MediaFileId) -> File
): MediaPlayer {
    return MediaPlayerImpl(getFile)
}

private class MediaPlayerImpl(
    getFile: suspend (MediaFileId) -> File
) : LocalMediaPlayer(getFile) {
    override fun createAudioOutput(frame: Frame): AudioOutput {
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize) // TODO
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        return AudioOutputImpl(audioTrack)
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
        }

        override fun stop() {
            out.stop()
        }

        override fun flush() {
            out.flush()
        }

        override fun release() {
            out.release()
        }
    }
}
