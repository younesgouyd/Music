package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class Artist(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface ArtistDao {
    @Query("select * from artist where id = :id")
    fun get(id: Long): Flow<Artist>

    @Query("select * from artist where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<Artist>>

    @Query(
        """
        select a.*
        from artist a
        join artisttrackcrossref cr on cr.artistId = a.id
        where cr.trackId = :trackId
    """
    )
    fun getTrackArtists(trackId: Long): Flow<List<Artist>>

    @Query("select * from artist where name = :name")
    fun getByName(name: String): Flow<List<Artist>>

    @Query(
        """
        insert into artist (name, creationDatetime, updateDatetime)
        values (:name, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(name: String, creationDatetime: Long, updateDatetime: Long): Long

    @Query("update artist set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: Long)

    @Query("delete from artist where id = :id")
    suspend fun delete(id: Long)
}