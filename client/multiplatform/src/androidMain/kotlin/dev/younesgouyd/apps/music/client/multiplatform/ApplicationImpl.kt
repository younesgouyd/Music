package dev.younesgouyd.apps.music.client.multiplatform

import android.content.Context
import java.io.File

class ApplicationImpl(
    context: Context
) : Application(
    File(context.filesDir, APP_DIR_NAME)
)