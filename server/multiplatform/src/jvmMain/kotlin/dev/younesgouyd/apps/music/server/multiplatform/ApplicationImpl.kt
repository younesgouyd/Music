package dev.younesgouyd.apps.music.server.multiplatform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

class ApplicationImpl : Application(
    File(
        File(System.getProperty("user.home")),
        APP_DIR_NAME
    )
) {
    override fun createDatabaseInstance(dbFile: File): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}