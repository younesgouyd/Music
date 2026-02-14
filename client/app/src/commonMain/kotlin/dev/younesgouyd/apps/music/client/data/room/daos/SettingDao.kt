package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.data.room.entities.Setting
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SettingDao {
    @Query("""
        select 1 from setting where name = 'dark_theme'
    """)
    abstract suspend fun darkThemeExists(): Int?

    @Query("""
        select 1 from setting where name = 'server_address'
    """)
    abstract suspend fun serverAddressExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_client_id'
    """)
    abstract suspend fun spotifyClientIdExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_client_secret'
    """)
    abstract suspend fun spotifyClientSecretExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_access_token'
    """)
    abstract suspend fun spotifyAccessTokenExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_token_expiration_time'
    """)
    abstract suspend fun spotifyTokenExpirationTimeExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_token_datetime'
    """)
    abstract suspend fun spotifyTokenDatetimeExists(): Int?

    @Query("select * from setting where name = 'dark_theme'")
    abstract fun getDarkTheme(): Flow<Setting>

    @Query("select * from setting where name = 'server_address'")
    abstract fun getServerAddress(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_client_id'")
    abstract fun getSpotifyClientId(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_client_secret'")
    abstract fun getSpotifyClientSecret(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_access_token'")
    abstract fun getSpotifyAccessToken(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_token_expiration_time'")
    abstract fun getSpotifyTokenExpirationTime(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_token_datetime'")
    abstract fun getSpotifyTokenDatetime(): Flow<Setting>

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('dark_theme', :darkTheme, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initDarkTheme(
        darkTheme: DarkThemeOptions = DarkThemeOptions.Enabled,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('server_address', :address, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initServerAddress(
        address: String = "http://0.0.0.0:8080/Music",
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_client_id', '', :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initSpotifyClientId(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_client_secret', '', :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initSpotifyClientSecret(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_access_token', '', :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initSpotifyAccessToken(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_token_expiration_time', '', :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initSpotifyTokenExpirationTime(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_token_datetime', '', :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun initSpotifyTokenDatetime(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("update setting set value = :darkTheme, updateDatetime = :updateDatetime where name = 'dark_theme'")
    abstract suspend fun updateDarkTheme(
        darkTheme: DarkThemeOptions,
        updateDatetime: Long
    )

    @Query("update setting set value = :address, updateDatetime = :updateDatetime where name = 'server_address'")
    abstract suspend fun updateServerAddress(address: String?, updateDatetime: Long)

    @Transaction
    open suspend fun updateSpotifyCredentials(clientId: String, clientSecret: String) {
        updateSpotifyClientId(clientId, System.currentTimeMillis())
        updateSpotifyClientSecret(clientSecret, System.currentTimeMillis())
    }

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_client_id'")
    abstract suspend fun updateSpotifyClientId(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_client_secret'")
    abstract suspend fun updateSpotifyClientSecret(value: String, updateDatetime: Long)

    @Transaction
    open suspend fun updateSpotifyToken(accessToken: String, expirationTimeSeconds: String, creationDatetimeEpochSecond: String) {
        updateSpotifyAccessToken(accessToken, System.currentTimeMillis())
        updateSpotifyTokenExpirationTime(expirationTimeSeconds, System.currentTimeMillis())
        updateSpotifyTokenDatetime(creationDatetimeEpochSecond, System.currentTimeMillis())
    }

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_access_token'")
    abstract suspend fun updateSpotifyAccessToken(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_token_expiration_time'")
    abstract suspend fun updateSpotifyTokenExpirationTime(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_token_datetime'")
    abstract suspend fun updateSpotifyTokenDatetime(value: String, updateDatetime: Long)
}