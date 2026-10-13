package dev.younesgouyd.apps.music.server.multiplatform

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

class ApplicationImpl(
    private val context: Context
) : Application(
    File(context.filesDir, APP_DIR_NAME)
) {
    override fun createDatabaseInstance(dbFile: File): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(context = context, name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}