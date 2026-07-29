package dev.younesgouyd.apps.music.server.common

import dev.younesgouyd.apps.music.server.common.data.room.AppDatabase
import java.io.File

expect fun createDatabaseInstance(dbFile: File): AppDatabase