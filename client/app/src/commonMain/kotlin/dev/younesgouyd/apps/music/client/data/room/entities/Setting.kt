package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.SettingId
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.flow.Flow

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
data class Setting(
    @PrimaryKey(autoGenerate = true)
    val id: SettingId,
    val name: String,
    val value: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface SettingDao {
    @Query("""
        select 1 from setting where name = 'dark_theme'
    """)
    suspend fun darkThemeExists(): Int?

    @Query("""
        select 1 from setting where name = 'server_address'
    """)
    suspend fun serverAddressExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_client_id'
    """)
    suspend fun spotifyClientIdExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_client_secret'
    """)
    suspend fun spotifyClientSecretExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_access_token'
    """)
    suspend fun spotifyAccessTokenExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_token_expiration_time'
    """)
    suspend fun spotifyTokenExpirationTimeExists(): Int?

    @Query("""
        select 1 from setting where name = 'spotify_token_datetime'
    """)
    suspend fun spotifyTokenDatetimeExists(): Int?

    @Query("select * from setting where name = 'dark_theme'")
    fun getDarkTheme(): Flow<Setting>

    @Query("select * from setting where name = 'server_address'")
    fun getServerAddress(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_client_id'")
    fun getSpotifyClientId(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_client_secret'")
    fun getSpotifyClientSecret(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_access_token'")
    fun getSpotifyAccessToken(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_token_expiration_time'")
    fun getSpotifyTokenExpirationTime(): Flow<Setting>

    @Query("select * from setting where name = 'spotify_token_datetime'")
    fun getSpotifyTokenDatetime(): Flow<Setting>

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('dark_theme', :darkTheme, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun initDarkTheme(
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
    suspend fun initServerAddress(
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
    suspend fun initSpotifyClientId(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_client_secret', '', :creationDatetime, :updateDatetime)
    """
    )
    suspend fun initSpotifyClientSecret(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_access_token', '', :creationDatetime, :updateDatetime)
    """
    )
    suspend fun initSpotifyAccessToken(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_token_expiration_time', '', :creationDatetime, :updateDatetime)
    """
    )
    suspend fun initSpotifyTokenExpirationTime(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values ('spotify_token_datetime', '', :creationDatetime, :updateDatetime)
    """
    )
    suspend fun initSpotifyTokenDatetime(
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("update setting set value = :darkTheme, updateDatetime = :updateDatetime where name = 'dark_theme'")
    suspend fun updateDarkTheme(
        darkTheme: DarkThemeOptions,
        updateDatetime: Long
    )

    @Query("update setting set value = :address, updateDatetime = :updateDatetime where name = 'server_address'")
    suspend fun updateServerAddress(address: String?, updateDatetime: Long)

    @Transaction
    suspend fun updateSpotifyCredentials(clientId: String, clientSecret: String) {
        updateSpotifyClientId(clientId, System.currentTimeMillis())
        updateSpotifyClientSecret(clientSecret, System.currentTimeMillis())
    }

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_client_id'")
    suspend fun updateSpotifyClientId(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_client_secret'")
    suspend fun updateSpotifyClientSecret(value: String, updateDatetime: Long)

    @Transaction
    suspend fun updateSpotifyToken(accessToken: String, expirationTimeSeconds: String, creationDatetimeEpochSecond: String) {
        updateSpotifyAccessToken(accessToken, System.currentTimeMillis())
        updateSpotifyTokenExpirationTime(expirationTimeSeconds, System.currentTimeMillis())
        updateSpotifyTokenDatetime(creationDatetimeEpochSecond, System.currentTimeMillis())
    }

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_access_token'")
    suspend fun updateSpotifyAccessToken(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_token_expiration_time'")
    suspend fun updateSpotifyTokenExpirationTime(value: String, updateDatetime: Long)

    @Query("update setting set value = :value, updateDatetime = :updateDatetime where name = 'spotify_token_datetime'")
    suspend fun updateSpotifyTokenDatetime(value: String, updateDatetime: Long)
}