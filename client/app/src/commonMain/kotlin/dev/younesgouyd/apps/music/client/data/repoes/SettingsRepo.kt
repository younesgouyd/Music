package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.Setting
import dev.younesgouyd.apps.music.client.data.room.entities.SettingDao
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.flow.Flow

class SettingsRepo(
    private val dao: SettingDao
) {
    suspend fun init() {
        if (dao.darkThemeExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initDarkTheme(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.serverAddressExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initServerAddress(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.spotifyClientIdExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initSpotifyClientId(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.spotifyClientSecretExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initSpotifyClientSecret(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.spotifyAccessTokenExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initSpotifyAccessToken(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.spotifyTokenExpirationTimeExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initSpotifyTokenExpirationTime(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
        if (dao.spotifyTokenDatetimeExists() == null) {
            val currentTime = System.currentTimeMillis()
            dao.initSpotifyTokenDatetime(
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
    }

    fun getDarkTheme(): Flow<Setting> {
        return dao.getDarkTheme()
    }

    fun getServerAddress(): Flow<Setting> {
        return dao.getServerAddress()
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

    suspend fun updateServerAddress(address: String?) {
        dao.updateServerAddress(
            address = if (address.isNullOrBlank()) null else address.trim(),
            updateDatetime = System.currentTimeMillis()
        )
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