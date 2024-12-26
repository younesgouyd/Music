package dev.younesgouyd.apps.music.app.components.util.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.time.Instant

typealias ImageUrl = String

@Composable
fun Image(
    modifier: Modifier = Modifier,
    url: ImageUrl?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    var loading by remember { mutableStateOf(true) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    if (url == null) {
        BrokenImage(modifier)
    } else {
        LaunchedEffect(url) {
            loading = true
            image = Cache.get(url)
            loading = false
        }

        when (loading) {
            true -> LoadingImage(modifier)
            false -> {
                image?.let {
                    androidx.compose.foundation.Image(
                        modifier = modifier,
                        bitmap = it,
                        contentDescription = null,
                        contentScale = contentScale,
                        alignment = alignment
                    )
                } ?: BrokenImage(modifier)
            }
        }
    }
}

@Composable
private fun LoadingImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = { CircularProgressIndicator() }
    )
}

@Composable
private fun BrokenImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null
        )
    }
}

private object Cache {
    private const val MAX_CACHE_SIZE = 100 * 1024 * 1024

    private val cache = mutableMapOf<ImageUrl, Image>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cacheSize = 0

    suspend fun get(url: ImageUrl): ImageBitmap {
        return withContext(Dispatchers.IO) {
            val fromCache = cache[url]
            if (fromCache == null) {
                mutex.withLock {
                    val fromCache2 = cache[url]
                    if (fromCache2 == null) {
                        URI(url).toURL().openStream().use { inputStream ->
                            val imageBitmap = loadImageBitmap(inputStream)
                            val image = Image(imageBitmap)
                            add(url to image)
                            while (cacheSize > MAX_CACHE_SIZE) {
                                val leastImportant = cache.minByOrNull { it.value.lastUsed }
                                if (leastImportant != null) {
                                    remove(leastImportant.toPair())
                                }
                            }
                            image.bitmap
                        }
                    } else {
                        fromCache2.updateLastUsed()
                        fromCache2.bitmap
                    }
                }
            } else {
                fromCache.updateLastUsed()
                fromCache.bitmap
            }
        }
    }

    private suspend fun add(entry: Pair<ImageUrl, Image>) {
        scope.launch {
            cache += entry
            cacheSize += entry.second.byteSize
        }.join()
    }

    private suspend fun remove(entry: Pair<ImageUrl, Image>) {
        scope.launch {
            cache.remove(entry.first)
            cacheSize -= entry.second.byteSize
        }.join()
    }

    private data class Image(val bitmap: ImageBitmap) {
        var lastUsed = Instant.now().toEpochMilli()
        val byteSize = bitmap.asSkiaBitmap().computeByteSize()

        fun updateLastUsed() { lastUsed = Instant.now().toEpochMilli() }
    }
}
