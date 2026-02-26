package dev.younesgouyd.apps.music.client.app.multiplatform.components.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
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
import java.net.URI
import java.net.URL
import java.time.Instant

@Composable
fun Image(
    modifier: Modifier = Modifier,
    file: File?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    val image = produceState<Result<ImageBitmap>>(Result.Loading, file?.path) {
        value = if (file?.path == null) Result.Error else Result.Success(Cache.get(Key.File(file.path)))
    }

    when (val img = image.value) {
        is Result.Error -> BrokenImage(modifier)
        is Result.Loading -> BrokenImage(modifier)
        is Result.Success<ImageBitmap> -> Image(
            modifier = modifier,
            bitmap = img.value,
            contentDescription = null,
            contentScale = contentScale,
            alignment = alignment
        )
    }
}

@Composable
fun Image(
    modifier: Modifier = Modifier,
    url: String?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    val image = produceState<Result<ImageBitmap>>(Result.Loading, url) {
        value = if (url == null) Result.Error else Result.Success(Cache.get(Key.Url(url)))
    }

    when (val img = image.value) {
        is Result.Error -> BrokenImage(modifier)
        is Result.Loading -> BrokenImage(modifier)
        is Result.Success<ImageBitmap> -> Image(
            modifier = modifier,
            bitmap = img.value,
            contentDescription = null,
            contentScale = contentScale,
            alignment = alignment
        )
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
            true -> BrokenImage(modifier)
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
private fun BrokenImage(
    modifier: Modifier
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

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    object Error : Result<Nothing>()
    class Success<T>(val value: T) : Result<T>()
}

sealed class Key {
    data class File(val path: String) : Key()
    data class Url(val url: String) : Key()
}

private object Cache {
    private const val MAX_CACHE_SIZE = 100 * 1024 * 1024

    private val cache = mutableMapOf<Key, Image>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cacheSize = 0

    suspend fun get(key: Key): ImageBitmap {
        return withContext(Dispatchers.IO) {
            val fromCache = cache[key]
            if (fromCache == null) {
                mutex.withLock {
                    val fromCache2 = cache[key]
                    if (fromCache2 == null) {
                        val imageBitmap = when (key) {
                            is Key.File -> File(key.path).readBytes2().decodeToImageBitmap()
                            is Key.Url -> URI(key.url).toURL().readBytes2().decodeToImageBitmap()
                        }
                        val image = Image(imageBitmap)
                        add(key to image)
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

    private suspend fun add(entry: Pair<Key, Image>) {
        scope.launch {
            cache += entry
            cacheSize += entry.second.byteSize
        }.join()
    }

    private suspend fun remove(entry: Pair<Key, Image>) {
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

expect fun File.readBytes2(): ByteArray

expect fun URL.readBytes2(): ByteArray