package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.SettingId
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
@Serializable
data class Setting(
    @PrimaryKey(autoGenerate = true)
    val id: SettingId,
    val name: String,
    val value: String?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface SettingDao {
    @Query("select * from setting")
    fun getAll(): Flow<List<Setting>>

    @Query("select * from setting where name = 'dark_theme'")
    fun getDarkTheme(): Flow<Setting?>

    @Query("select * from setting where name = 'server_address'")
    fun getServerAddress(): Flow<Setting?>

    @Query(
        """
        insert into setting (name, value, creationDatetime, updateDatetime)
        values (:name, :value, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        name: String,
        value: String,
        creationDatetime: Long,
        updateDatetime: Long
    )

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

    @Query("update setting set value = :darkTheme, updateDatetime = :updateDatetime where name = 'dark_theme'")
    suspend fun updateDarkTheme(
        darkTheme: DarkThemeOptions,
        updateDatetime: Long
    )

    @Query("update setting set value = :address, updateDatetime = :updateDatetime where name = 'server_address'")
    suspend fun updateServerAddress(address: String?, updateDatetime: Long)
}