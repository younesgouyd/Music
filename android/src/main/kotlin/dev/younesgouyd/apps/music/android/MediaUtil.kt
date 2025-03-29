package dev.younesgouyd.apps.music.android

import android.content.Context
import dev.younesgouyd.apps.music.common.util.MediaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media

class MediaUtil(private val context: Context) : MediaUtil() {
    override suspend fun getDuration(mediaPath: String): Long {
        return withContext(Dispatchers.IO) {
            val libVlc = LibVLC(context)
            val media = Media(libVlc, mediaPath)
            try {
                media.duration
            } finally {
                media.release()
                libVlc.release()
            }
        }
    }
}
