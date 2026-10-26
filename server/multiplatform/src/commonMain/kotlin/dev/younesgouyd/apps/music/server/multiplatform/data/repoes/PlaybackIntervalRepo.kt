package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.PlaybackIntervalDao

class PlaybackIntervalRepo(
    val dao: PlaybackIntervalDao
) {
    suspend fun add(
        audioFileId: MediaFileId,
        clientId: ClientId,
        importId: ImportSessionItemId,
        trackId: TrackId,
        playlistId: PlaylistId?,
        folderId: FolderId?,
        albumId: SpotifyAlbumId?,
        artistId: SpotifyArtistId?,
        from: Long,
        to: Long,
        startDatetime: Long,
        endDatetime: Long
    ) {
        dao.add(
            audioFileId = audioFileId,
            clientId = clientId,
            importId = importId,
            trackId = trackId,
            playlistId = playlistId,
            folderId = folderId,
            albumId = albumId,
            artistId = artistId,
            from = from,
            to = to,
            startDatetime = startDatetime,
            endDatetime = endDatetime,
            creationDatetime = System.currentTimeMillis()
        )
    }
}