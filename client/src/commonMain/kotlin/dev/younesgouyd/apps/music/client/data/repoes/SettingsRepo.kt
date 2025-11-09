package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.Setting
import dev.younesgouyd.apps.music.client.data.room.entities.SettingDao
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepo(
    private val dao: SettingDao
) {
    suspend fun init() {
        val darkTheme = getDarkTheme().first()
        if (darkTheme == null) {
            val currentTime = System.currentTimeMillis()
            dao.initDarkTheme(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        val address = getServerAddress().first()
        if (address == null) {
            val currentTime = System.currentTimeMillis()
            dao.initServerAddress(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
    }

    fun getDarkTheme(): Flow<Setting?> {
        return dao.getDarkTheme()
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        dao.updateDarkTheme(darkTheme = theme, System.currentTimeMillis())
    }

    fun getServerAddress(): Flow<Setting?> {
        return dao.getServerAddress()
    }

    suspend fun updateServerAddress(address: String?) {
        dao.updateServerAddress(
            address = if (address.isNullOrBlank()) null else address.trim(),
            updateDatetime = System.currentTimeMillis()
        )
    }
}