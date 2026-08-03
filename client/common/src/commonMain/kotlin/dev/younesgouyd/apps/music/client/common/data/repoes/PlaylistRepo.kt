package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.PlaylistRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlaylistRepo(
    private val backend: Backend
) {
    suspend fun getAll(limit: Int, offset: Offset.Id<PlaylistId>): List<Playlist> {
        return backend.call(
            PlaylistRpc.GetAll(
                limit = limit,
                offset = offset
            )
        ).body<List<Playlist>>()
    }

    fun get(id: PlaylistId): Flow<Playlist?> {
        return flow {
            emit(
                backend.call(
                    PlaylistRpc.Get(id)
                ).body<Playlist?>()
            )
        }
    }

    fun search(nameQuery: String): Flow<List<Playlist>> {
        return flow {
            emit(
                backend.call(
                    PlaylistRpc.Search(nameQuery)
                ).body<List<Playlist>>()
            )
        }
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Playlist>> {
        return flow {
            emit(
                backend.call(
                    PlaylistRpc.SearchFolder(
                        folderId = folderId,
                        nameQuery = nameQuery
                    )
                ).body<List<Playlist>>()
            )
        }
    }

    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>> {
        return flow {
            emit(
                backend.call(
                    PlaylistRpc.GetFolderPlaylists(folderId)
                ).body<List<Playlist>>()
            )
        }
    }

    fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>> {
        return flow {
            emit(
                backend.call(
                    PlaylistRpc.GetTrackPlaylists(id)
                ).body<List<Playlist>>()
            )
        }
    }

    suspend fun add(
        name: String,
        folderId: FolderId?
    ): PlaylistId {
        require(name.isNotEmpty())
        return backend.call(
            PlaylistRpc.Add(
                name = name,
                folderId = folderId
            )
        ).body<PlaylistId>()
    }

    suspend fun updateName(id: PlaylistId, name: String) {
        require(name.isNotEmpty())
        backend.call(
            PlaylistRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateFolderId(id: PlaylistId, folderId: FolderId) {
        backend.call(
            PlaylistRpc.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }

    suspend fun delete(id: PlaylistId) {
        backend.call(
            PlaylistRpc.Delete(id)
        )
    }
}