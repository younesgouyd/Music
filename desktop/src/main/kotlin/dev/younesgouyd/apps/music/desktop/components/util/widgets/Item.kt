package dev.younesgouyd.apps.music.desktop.components.util.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// todo - currently we can't add margin because the modifier is passed to the inner Box.
//        this decision was made to control content padding, and content Alignment when Card's size is greater
//        than its content.
@Composable
fun Item(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            content = {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = contentAlignment,
                    content = { content() }
                )
            },
            onClick = onClick,
            elevation = CardDefaults.elevatedCardElevation(),
            colors = CardDefaults.elevatedCardColors()
        )
    } else {
        Card(
            content = {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = contentAlignment,
                    content = { content() }
                )
            },
            elevation = CardDefaults.elevatedCardElevation(),
            colors = CardDefaults.elevatedCardColors()
        )
    }
}
