package dev.younesgouyd.apps.music.client.app.multiplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicAndroidApp.music.show(
                Modifier.fillMaxSize()
                    .systemBarsPadding()
            )
        }
    }

    // TODO
//    override fun onDestroy() {
//        super.onDestroy()
//        MusicAndroidApp.music.clear()
//    }
}
