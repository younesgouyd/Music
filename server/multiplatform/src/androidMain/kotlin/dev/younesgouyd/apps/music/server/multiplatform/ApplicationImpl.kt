package dev.younesgouyd.apps.music.server.multiplatform

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.multiplatform.data.room.MIGRATION_1_2
import java.io.File

class ApplicationImpl(
    context: Context
) : Application(
    File(context.filesDir, APP_DIR_NAME)
) {
    override fun createDatabaseInstance(dbFile: File): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}