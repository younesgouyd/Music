package dev.younesgouyd.apps.music.client.multiplatform

import java.io.File

class ApplicationImpl : Application(
    File(
        File(System.getProperty("user.home")),
        APP_DIR_NAME
    )
)