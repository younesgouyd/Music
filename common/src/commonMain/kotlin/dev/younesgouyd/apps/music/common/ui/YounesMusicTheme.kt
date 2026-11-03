package dev.younesgouyd.apps.music.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.models.DarkThemeOptions

@Composable
fun YounesMusicTheme(
    darkTheme: DarkThemeOptions = DarkThemeOptions.SystemDefault,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when (darkTheme) {
            DarkThemeOptions.SystemDefault -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            DarkThemeOptions.Disabled -> lightColorScheme()
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