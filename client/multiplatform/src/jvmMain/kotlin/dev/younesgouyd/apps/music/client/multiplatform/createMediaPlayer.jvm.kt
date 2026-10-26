package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

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
        val audioFormat = AudioFormat(
            /* encoding = */ AudioFormat.Encoding.PCM_SIGNED,
            /* sampleRate = */ SAMPLE_RATE.toFloat(),
            /* sampleSizeInBits = */ BIT_DEPTH,
            /* channels = */ CHANNELS,
            /* frameSize = */ (BIT_DEPTH / 8) * CHANNELS,
            /* frameRate = */ SAMPLE_RATE.toFloat(),
            /* bigEndian = */ false
        )
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val l = AudioSystem.getLine(info) as SourceDataLine
        l.open(audioFormat)
        return AudioOutputImpl(l)
    }

    private class AudioOutputImpl(
        val out: SourceDataLine
    ) : AudioOutput() {
        private var byteArray: ByteArray = ByteArray(0)
        private var byteBuffer: ByteBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
        private var bufferView: ShortBuffer = byteBuffer.asShortBuffer()

        override fun play() {
            out.start()
        }

        override fun write(samples: ShortBuffer) {
            val count = samples.remaining()
            val byteCount = samples.remaining() * 2
            if (byteArray.size < byteCount) {
                byteArray = ByteArray(byteCount)
                byteBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
                bufferView = byteBuffer.asShortBuffer()
            }
            bufferView.clear()
            bufferView.put(samples)
            out.write(byteArray, 0, byteCount)
            totalShortsWritten += count
        }

        override fun stop() {
            out.drain()
            out.stop()
        }

        override fun drain() {
            out.drain()
        }

        override fun release() {
            out.close()
        }
    }
}
