package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.Album
import dev.younesgouyd.apps.music.common.data.room.entities.AlbumDao
import kotlinx.coroutines.flow.Flow

class AlbumRepo(private val dao: AlbumDao) {
    fun getAll(): Flow<List<Album>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Album> {
        return dao.get(id)
    }

    fun getArtistAlbums(artistId: Long): Flow<List<Album>> {
        return dao.getArtistAlbums(artistId)
    }

    fun getByName(name: String): Flow<List<Album>> {
        return dao.getByName(name)
    }

    suspend fun add(name: String, image: ByteArray?, releaseDate: String?): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            image = image,
            releaseDate = releaseDate,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateImage(id: Long, image: ByteArray?) {
        dao.updateImage(image, System.currentTimeMillis(), id)
    }

    suspend fun updateReleaseDate(id: Long, releaseDate: String?) {
        dao.updateReleaseDate(releaseDate, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}