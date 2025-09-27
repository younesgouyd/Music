package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Setting
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.SettingQueries
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SettingsRepo(
    private val queries: SettingQueries
) {
    suspend fun init() {
        val darkTheme = getDarkTheme().firstOrNull()
        if (darkTheme == null) {
            val currentTime = System.currentTimeMillis()
            queries.initDarkTheme(
                value = DarkThemeOptions.SystemDefault.name,
                creation_datetime = currentTime,
                update_datetime = currentTime
            )
        }
        val address = getServerAddress().firstOrNull()
        if (address == null) {
            val currentTime = System.currentTimeMillis()
            queries.initServerAddress(
                value = "http://0.0.0.0:8080/Music",
                creation_datetime = currentTime,
                update_datetime = currentTime
            )
        }
    }

    fun getDarkTheme(): Flow<Setting?> {
        return queries.getDarkTheme()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        withContext(Dispatchers.IO) {
            queries.updateDarkTheme(value = theme.name, System.currentTimeMillis())
        }
    }

    fun getServerAddress(): Flow<Setting?> {
        return queries.getServerAddress()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun updateServerAddress(address: String?) {
        withContext(Dispatchers.IO) {
            queries.updateServerAddress(
                value = if (address.isNullOrBlank()) null else address.trim(),
                update_datetime =System.currentTimeMillis()
            )
        }
    }
}