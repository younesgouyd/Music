package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val image: ByteArray?,
    val releaseDate: String?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface AlbumDao {
    @Query("select * from album")
    fun getAll(): Flow<List<Album>>

    @Query("select * from album where id = :id")
    fun get(id: Long): Flow<Album>

    @Query("""
        select distinct a.*
        from album a
        join track t on t.albumId = a.id
        join artisttrackcrossref atcr on atcr.trackId = t.id
        join artist at on at.id = atcr.artistId
        where at.id = :artistId
    """)
    fun getArtistAlbums(artistId: Long): Flow<List<Album>>

    @Query("select * from album where name = :name")
    fun getByName(name: String): Flow<List<Album>>

    @Query("""
        insert into album (name, image, releaseDate, creationDatetime, updateDatetime)
        values (:name, :image, :releaseDate, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        name: String,
        image: ByteArray?,
        releaseDate: String?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update album set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: Long)

    @Query("update album set image = :image, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateImage(image: ByteArray?, updateDatetime: Long, id: Long)

    @Query("update album set releaseDate = :releaseDate, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateReleaseDate(releaseDate: String?, updateDatetime: Long, id: Long)

    @Query("delete from album where id = :id")
    suspend fun delete(id: Long)
}