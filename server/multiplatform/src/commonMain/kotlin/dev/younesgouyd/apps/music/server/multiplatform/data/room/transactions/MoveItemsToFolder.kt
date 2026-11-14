package dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId

@Dao
abstract class MoveItemsToFolder {
    @Transaction
    open suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        folder: FolderId?
    ) {
        moveTracks(folderId = folder, ids = tracks)
        movePlaylists(folderId = folder, ids = playlists)
        moveFolders(folderId = folder, ids = folders)
    }


    @Query("""
        update track
        set folderId = :folderId,
            updateDatetime = :updateDatetime
        where id in (:ids)
    """)
    protected abstract suspend fun moveTracks(
        folderId: FolderId?,
        updateDatetime: Long = System.currentTimeMillis(),
        ids: Set<TrackId>
    )

    @Query("""
        update playlist
        set folderId = :folderId,
            updateDatetime = :updateDatetime
        where id in (:ids)
    """)
    protected abstract suspend fun movePlaylists(
        folderId: FolderId?,
        updateDatetime: Long = System.currentTimeMillis(),
        ids: Set<PlaylistId>
    )

    @Query("""
        update folder
        set parentFolderId = :folderId,
            updateDatetime = :updateDatetime
        where id in (:ids)
    """)
    protected abstract suspend fun moveFolders(
        folderId: FolderId?,
        updateDatetime: Long = System.currentTimeMillis(),
        ids: Set<FolderId>
    )
}