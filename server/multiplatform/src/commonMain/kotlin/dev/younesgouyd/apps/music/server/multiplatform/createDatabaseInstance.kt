package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import java.io.File

expect fun createDatabaseInstance(dbFile: File): AppDatabase