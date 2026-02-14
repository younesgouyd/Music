package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.PlaylistTrack
import dev.younesgouyd.apps.music.client.data.room.entities.Track
import dev.younesgouyd.apps.music.client.data.room.entities.TrackDao
import dev.younesgouyd.apps.music.client.data.room.entities.TrackRelation
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val dao: TrackDao
) {
    fun getAll(): Flow<List<TrackRelation>> {
        return dao.getAll()
    }

    fun get(id: TrackId): Flow<TrackRelation?> {
        return dao.get(id)
    }

    fun search(nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<TrackRelation>> {
        return dao.search(nameQuery.toSearchQuery(), tags, includeUntagged)
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery(), tags, includeUntagged)
    }

    fun searchArtistContributions(id: SpotifyArtistId, nameQuery: String): Flow<List<TrackRelation>> {
        return dao.searchArtistContributions(id, nameQuery.toSearchQuery())
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        return dao.searchPlaylist(id, nameQuery.toSearchQuery())
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        return dao.searchWithTag(nameQuery.toSearchQuery(), tag)
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        return dao.getFolderTracks(id)
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        return dao.getArtistTracks(id)
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        return dao.getAlbumTracks(id)
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        return dao.getPlaylistTracks(id)
    }

    suspend fun getId(spotifyId: String): TrackId? {
        return dao.getId(spotifyId)?.let { TrackId(it) }
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        return dao.getImportSessionTrack(id)
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            importSessionItemId = importSessionItemId,
            spotifyTrackId = spotifyTrackId,
            folderId = folderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return TrackId(id)
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }
}