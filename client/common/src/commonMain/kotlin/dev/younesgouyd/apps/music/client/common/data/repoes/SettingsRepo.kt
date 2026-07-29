package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.DarkThemeOptions
import dev.younesgouyd.apps.music.common.Setting
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class SettingsRepo(
    private val client: HttpClient
) {
    fun getDarkTheme(): Flow<Setting> {
        TODO()
//        return dao.getDarkTheme()
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        TODO()
//        dao.updateDarkTheme(darkTheme = theme, System.currentTimeMillis())
    }
}