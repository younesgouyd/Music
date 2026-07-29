package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.*
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class PlaylistRepo(
    private val client: HttpClient
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        TODO()
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        TODO()
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        TODO()
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        TODO()
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        TODO()
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        TODO()
    }

    suspend fun add(
        name: String,
        folderId: FolderId?,
    ): PlaylistId {
        require(name.isNotEmpty())
        TODO()
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        TODO()
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        TODO()
    }

    suspend fun delete(id: PlaylistId) {
        TODO()
    }
}