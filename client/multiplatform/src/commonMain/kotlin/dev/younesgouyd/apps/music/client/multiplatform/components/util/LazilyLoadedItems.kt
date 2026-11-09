package dev.younesgouyd.apps.music.client.multiplatform.components.util

import dev.younesgouyd.apps.music.common.models.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

typealias PageSize = Int

class LazilyLoadedItems<Item, OffsetType : Offset>(
    private val scope: CoroutineScope,
    private val loader: StateFlow<Loader<Item, OffsetType>>,
    initialOffset: OffsetType
) {
    companion object {
        const val PAGE_SIZE = 30
    }

    constructor(
        scope: CoroutineScope,
        loader: Loader<Item, OffsetType>,
        initialOffset: OffsetType
    ) : this(scope, MutableStateFlow(loader), initialOffset)

    private val _items: MutableStateFlow<List<Item>> = MutableStateFlow(emptyList())
    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private var nextOffset: OffsetType? = initialOffset
    private var loadingJob: Job? = null
    private var initFlag = true

    val items: StateFlow<List<Item>> get() = _items.asStateFlow()
    val loading: StateFlow<Boolean> get() = _loading.asStateFlow()

    init {
        scope.launch(Dispatchers.Main) {
            loader.collectLatest {
                initFlag = true
                loadingJob?.cancelAndJoin()
                loadingJob = null
                _loading.value = true
                _items.value = emptyList()
                nextOffset = initialOffset
                load(it)
                initFlag = false
            }
        }
    }

    suspend fun loadMore() {
        withContext(Dispatchers.Main) {
            if (initFlag || loadingJob?.isActive == true) return@withContext
            loadingJob = scope.launch(Dispatchers.Main) { load(loader.value) }
        }
    }

    private suspend fun load(loader: Loader<Item, OffsetType>) {
        nextOffset?.let { nextOffsetNotNull ->
            _loading.value = true
            val result = try {
                loader.load(nextOffsetNotNull, PAGE_SIZE)
            } catch (e: Exception) {
                _loading.value = false
                throw e
            }
            nextOffset = result.nextOffset
            _items.update {
                it + result.items
            }
            _loading.value = false
        }
    }

    fun interface Loader<Item, OffsetType : Offset> {
        suspend fun load(offset: OffsetType, pageSize: PageSize): Page<Item, OffsetType>
    }

    data class Page<Item, OffsetType: Offset>(
        val nextOffset: OffsetType?,
        val items: List<Item>
    )
}