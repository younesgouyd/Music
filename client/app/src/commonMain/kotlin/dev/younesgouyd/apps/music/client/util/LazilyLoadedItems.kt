package dev.younesgouyd.apps.music.client.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias PageSize = Int

class LazilyLoadedItems<Item, OffsetType : Offset>(
    private val coroutineScope: CoroutineScope,
    private val load: suspend (OffsetType, PageSize) -> Page<Item, OffsetType>,
    initialOffset: OffsetType
) {
    companion object {
        const val PAGE_SIZE = 5
    }
    private val _items: MutableStateFlow<List<Item>> = MutableStateFlow(emptyList())
    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var nextOffset: OffsetType? = initialOffset
    private val mutex = Mutex()

    val items: StateFlow<List<Item>> get() = _items.asStateFlow()
    val loading: StateFlow<Boolean> get() = _loading.asStateFlow()

    init {
        loadMore()
    }

    fun loadMore() {
        if (!_loading.value) {
            nextOffset?.let { nextOffsetNotNull ->
                coroutineScope.launch {
                    mutex.withLock {
                        _loading.value = true
                        val result = load(nextOffsetNotNull, PAGE_SIZE)
                        nextOffset = result.nextOffset
                        delay(2000)
                        _items.update {
                            it + result.items
                        }
                        _loading.value = false
                    }
                }
            }
        }
    }

    data class Page<Item, OffsetType: Offset>(
        val nextOffset: OffsetType?,
        val items: List<Item>
    )
}