package dev.younesgouyd.apps.music.desktop

import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

class MediaUtil : MediaUtil() {
    override suspend fun getDuration(mediaPath: String): Long {
        return withContext(Dispatchers.IO) {
            val factory = MediaPlayerFactory()
            val media = factory.media().newMedia(mediaPath)
            try {
                if (media.parsing().parse()) {
                    media.info().duration()
                } else {
                    TODO()
                }
            } finally {
                media.release()
                factory.release()
            }
        }
    }
}