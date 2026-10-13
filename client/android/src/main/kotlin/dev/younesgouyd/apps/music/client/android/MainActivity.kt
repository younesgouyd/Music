package dev.younesgouyd.apps.music.client.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.multiplatform.Application

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app: Application = (applicationContext as App).app
        enableEdgeToEdge()
        setContent {
            app.show(Modifier.fillMaxSize().systemBarsPadding())
        }
        onBackPressedDispatcher.addCallback(this) {
            app.navigateBack()
        }
    }
}
