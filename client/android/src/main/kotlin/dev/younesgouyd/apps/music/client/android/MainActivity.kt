package dev.younesgouyd.apps.music.client.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.common.Application

class MainActivity : ComponentActivity() {
    private lateinit var app: Application

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = Application()
        app.start()
        setContent {
            app.show(Modifier.fillMaxSize().systemBarsPadding())
        }
        onBackPressedDispatcher.addCallback(this) {
            app.navigateBack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.clear()
    }
}
