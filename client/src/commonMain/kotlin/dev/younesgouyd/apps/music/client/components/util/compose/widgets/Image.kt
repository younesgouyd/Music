package dev.younesgouyd.apps.music.client.components.util.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant

@Composable
fun Image(
    modifier: Modifier = Modifier,
    file: File?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    var loading by remember { mutableStateOf(true) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    if (file == null) {
        BrokenImage(modifier)
    } else {
        val path = file.path
        LaunchedEffect(path) {
            loading = true
            image = Cache.get(path)
            loading = false
        }

        when (loading) {
            true -> LoadingImage(modifier)
            false -> {
                image?.let {
                    Image(
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
fun Image(
    modifier: Modifier = Modifier,
    data: ByteArray?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    var loading by remember { mutableStateOf(true) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    if (data == null) {
        BrokenImage(modifier)
    } else {
        LaunchedEffect(data) {
            loading = true
            image = data.decodeToImageBitmap()
            loading = false
        }

        when (loading) {
            true -> LoadingImage(modifier)
            false -> {
                image?.let {
                    Image(
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
        Image(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null
        )
    }
}

private typealias FilePath = String

private object Cache {
    private const val MAX_CACHE_SIZE = 100 * 1024 * 1024

    private val cache = mutableMapOf<FilePath, Image>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cacheSize = 0

    suspend fun get(filePath: FilePath): ImageBitmap {
        return withContext(Dispatchers.IO) {
            val fromCache = cache[filePath]
            if (fromCache == null) {
                mutex.withLock {
                    val fromCache2 = cache[filePath]
                    if (fromCache2 == null) {
                        val imageBitmap = File(filePath)
                            .readBytes()
                            .decodeToImageBitmap()
                        val image = Image(imageBitmap)
                        add(filePath to image)
                        while (cacheSize > MAX_CACHE_SIZE) {
                            val leastImportant = cache.minByOrNull { it.value.lastUsed }
                            if (leastImportant != null) {
                                remove(leastImportant.toPair())
                            }
                        }
                        image.bitmap
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

    private suspend fun add(entry: Pair<FilePath, Image>) {
        scope.launch {
            cache += entry
            cacheSize += entry.second.byteSize
        }.join()
    }

    private suspend fun remove(entry: Pair<FilePath, Image>) {
        scope.launch {
            cache.remove(entry.first)
            cacheSize -= entry.second.byteSize
        }.join()
    }

    private data class Image(val bitmap: ImageBitmap) {
        var lastUsed = Instant.now().toEpochMilli()
        val byteSize = bitmap.width * bitmap.height * 4

        fun updateLastUsed() { lastUsed = Instant.now().toEpochMilli() }
    }
}