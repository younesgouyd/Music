package dev.younesgouyd.apps.music.common.util

abstract class MediaUtil {
    abstract suspend fun getDuration(mediaPath: String): Long
}