package dev.younesgouyd.apps.music.common.components.util.widgets

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.VerticalScrollbar(listState: LazyListState) {
    val adapter = rememberScrollbarAdapter(listState)

    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        adapter = adapter,
        style = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = MaterialTheme.colorScheme.secondary,
            hoverColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun BoxScope.VerticalScrollbar(listState: LazyGridState) {
    val adapter = rememberScrollbarAdapter(listState)

    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        adapter = adapter,
        style = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = MaterialTheme.colorScheme.secondary,
            hoverColor = MaterialTheme.colorScheme.primary
        )
    )
}