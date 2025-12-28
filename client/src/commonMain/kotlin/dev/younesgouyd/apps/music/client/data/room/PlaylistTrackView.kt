package dev.younesgouyd.apps.music.client.data.room

import androidx.room.DatabaseView
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId

@DatabaseView(
    viewName = "playlist_track_view",
    value = """
        select
            cr.id as playlistTrackCrossRefId,
            cr.playlistId,
            cr.trackId,
            cr.position,
            t.name,
            t.album
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
    """
)
data class PlaylistTrackView(
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId,
    val playlistId: PlaylistId,
    val trackId: TrackId,
    val position: Int,
    val name: String,
    val album: String?
)
