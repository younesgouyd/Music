package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.DarkThemeOptions
import dev.younesgouyd.apps.music.server.common.data.room.daos.SettingDao
import dev.younesgouyd.apps.music.server.common.data.room.entities.Setting
import kotlinx.coroutines.flow.Flow

class SettingRepo(
    private val dao: SettingDao
) {
    // TODO
    suspend fun init() {
    }

    fun getDarkTheme(): Flow<Setting> {
        return dao.getDarkTheme()
    }

    fun getSpotifyClientId(): Flow<Setting> {
        return dao.getSpotifyClientId()
    }

    fun getSpotifyClientSecret(): Flow<Setting> {
        return dao.getSpotifyClientSecret()
    }

    fun getSpotifyAccessToken(): Flow<Setting> {
        return dao.getSpotifyAccessToken()
    }

    fun getSpotifyTokenExpirationTime(): Flow<Setting> {
        return dao.getSpotifyTokenExpirationTime()
    }

    fun getSpotifyTokenDatetime(): Flow<Setting> {
        return dao.getSpotifyTokenDatetime()
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        dao.updateDarkTheme(darkTheme = theme, System.currentTimeMillis())
    }

    suspend fun updateSpotifyCredentials(clientId: String, clientSecret: String) {
        dao.updateSpotifyCredentials(clientId, clientSecret)
    }

    suspend fun updateSpotifyToken(accessToken: String, expirationTimeSeconds: String, creationDatetimeEpochSecond: String) {
        dao.updateSpotifyToken(
            accessToken = accessToken,
            expirationTimeSeconds = expirationTimeSeconds,
            creationDatetimeEpochSecond = creationDatetimeEpochSecond
        )
    }
}