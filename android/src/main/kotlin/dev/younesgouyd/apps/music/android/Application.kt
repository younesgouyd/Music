package dev.younesgouyd.apps.music.android

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic

class Application : Application() {
    private lateinit var _repoStore: RepoStore
    val repoStore = lazy { _repoStore }

    override fun onCreate() {
        super.onCreate()
        _repoStore = RepoStore(
            database = YounesMusic(AndroidSqliteDriver(schema = YounesMusic.Schema, context = this)),
            fileManager = FileManager(this)
        )
    }
}