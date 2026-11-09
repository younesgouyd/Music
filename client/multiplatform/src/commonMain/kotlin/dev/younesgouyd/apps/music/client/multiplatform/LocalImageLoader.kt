package dev.younesgouyd.apps.music.client.multiplatform

import androidx.compose.runtime.compositionLocalOf

val LocalImageLoader = compositionLocalOf<ImageLoader> {
    error("ImageLoader not provided.")
}