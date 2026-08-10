package dev.younesgouyd.apps.music.server.common

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.common.applicationContext
import dev.younesgouyd.apps.music.server.common.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun createDatabaseInstance(dbFile: File): AppDatabase {
    return Room.databaseBuilder<AppDatabase>(context = applicationContext, name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}