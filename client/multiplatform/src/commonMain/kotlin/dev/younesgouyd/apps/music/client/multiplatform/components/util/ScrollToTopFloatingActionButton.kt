package dev.younesgouyd.apps.music.client.multiplatform.components.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopFloatingActionButton(state: ScrollState) {
    val coroutineScope = rememberCoroutineScope()
    val visible by remember {
        derivedStateOf { state.value > 500 }
    }

    if (visible) {
        FloatingActionButton(
            content = { Icon(Icons.Default.ArrowUpward, null) },
            onClick = { coroutineScope.launch { state.animateScrollTo(0) } }
        )
    }
}

@Composable
fun ScrollToTopFloatingActionButton(state: LazyListState) {
    val coroutineScope = rememberCoroutineScope()
    val visible by remember {
        derivedStateOf { state.firstVisibleItemIndex > 5 }
    }

    if (visible) {
        FloatingActionButton(
            content = { Icon(Icons.Default.ArrowUpward, null) },
            onClick = { coroutineScope.launch { state.animateScrollToItem(0) } }
        )
    }
}

@Composable
fun ScrollToTopFloatingActionButton(state: LazyGridState) {
    val coroutineScope = rememberCoroutineScope()
    val visible by remember {
        derivedStateOf { state.firstVisibleItemIndex > 5 }
    }

    if (visible) {
        FloatingActionButton(
            content = { Icon(Icons.Default.ArrowUpward, null) },
            onClick = { coroutineScope.launch { state.animateScrollToItem(0) } }
        )
    }
}

@Composable
fun ScrollToTopFloatingActionButton(state: LazyStaggeredGridState) {
    val coroutineScope = rememberCoroutineScope()
    val visible by remember {
        derivedStateOf { state.firstVisibleItemIndex > 5 }
    }

    if (visible) {
        FloatingActionButton(
            content = { Icon(Icons.Default.ArrowUpward, null) },
            onClick = { coroutineScope.launch { state.animateScrollToItem(0) } }
        )
    }
}