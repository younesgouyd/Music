package dev.younesgouyd.apps.music.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions

@Composable
fun YounesMusicTheme(
    darkTheme: DarkThemeOptions,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = when (darkTheme) {
            DarkThemeOptions.SystemDefault -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            DarkThemeOptions.Disabled -> expressiveLightColorScheme()
            DarkThemeOptions.Enabled -> darkColorScheme()
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = { content() }
            )
        }
    }
}

@Composable
fun YounesMusicTheme(
    colorScheme: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = { content() }
            )
        }
    }
}