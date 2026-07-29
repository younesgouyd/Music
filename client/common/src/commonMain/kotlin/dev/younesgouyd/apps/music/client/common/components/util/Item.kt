package dev.younesgouyd.apps.music.client.common.components.util

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Item(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier.wrapContentSize()
                    .padding(contentPadding),
                contentAlignment = contentAlignment,
                content = { content() }
            )
        },
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors()
    )
}

@Composable
fun Item(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    if (onClick == null) {
        Item(
            modifier = modifier,
            contentAlignment = contentAlignment,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            content = {
                Box(
                    modifier = Modifier.wrapContentSize()
                        .padding(contentPadding),
                    contentAlignment = contentAlignment,
                    content = { content() }
                )
            },
            onClick = onClick,
            elevation = CardDefaults.elevatedCardElevation(),
            colors = CardDefaults.elevatedCardColors()
        )
    }
}

@Composable
fun Item(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier.wrapContentSize()
                    .padding(contentPadding)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                contentAlignment = contentAlignment,
                content = { content() }
            )
        },
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors()
    )
}

