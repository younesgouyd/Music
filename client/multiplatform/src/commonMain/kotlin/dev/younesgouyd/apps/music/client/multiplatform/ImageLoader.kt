package dev.younesgouyd.apps.music.client.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import dev.younesgouyd.apps.music.common.Platform
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.platform
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI
import java.time.Instant

class ImageLoader(
    private val mediaFileDownloader: MediaFileDownloader
) {
    companion object {
        private val MAX_CACHE_SIZE: Long = when (platform) {
            Platform.JVM -> 1024 * 1024 * 1024
            Platform.ANDROID -> 100 * 1024 * 1024
        }
    }

    private val logger = KotlinLogging.logger {}
    private val cache = mutableMapOf<Key, Image>()
    private val mutex = Mutex()
    private var cacheSize: Long = 0

    suspend fun get(key: Key): ImageBitmap {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val fromCache = cache[key]
                if (fromCache == null) {
                    val imageBitmap = try {
                        when (key) {
                            is Key.Mfid -> mediaFileDownloader.download(key.id).readBytes().decodeToImageBitmap()
                            is Key.Url -> URI(key.url).toURL().readBytes().decodeToImageBitmap()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) {}
                        throw e
                    }
                    val image = Image(imageBitmap)
                    add(key to image)
                    while (cacheSize > MAX_CACHE_SIZE) {
                        val leastImportant = cache.minByOrNull { it.value.lastUsed }
                        if (leastImportant != null) {
                            remove(leastImportant.toPair())
                        } else {
                            logger.error {
                                "cache accounting drifted: cacheSize=$cacheSize with an empty cache (max=$MAX_CACHE_SIZE). Resetting."
                            }
                            clear()
                            break
                        }
                    }
                    image.bitmap
                } else {
                    fromCache.updateLastUsed()
                    fromCache.bitmap
                }
            }
        }
    }

    private fun add(entry: Pair<Key, Image>) {
        cache += entry
        cacheSize += entry.second.byteSize
    }

    private fun remove(entry: Pair<Key, Image>) {
        cache.remove(entry.first)
        cacheSize -= entry.second.byteSize
    }

    private fun clear() {
        cache.clear()
        cacheSize = 0
    }

    private data class Image(val bitmap: ImageBitmap) {
        var lastUsed = Instant.now().toEpochMilli()
        val byteSize = bitmap.width * bitmap.height * 4

        fun updateLastUsed() { lastUsed = Instant.now().toEpochMilli() }
    }

    sealed class Key {
        data class Mfid(val id: MediaFileId) : Key()
        data class Url(val url: String) : Key()
    }
}