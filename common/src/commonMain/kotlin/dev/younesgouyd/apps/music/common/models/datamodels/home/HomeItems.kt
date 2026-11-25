package dev.younesgouyd.apps.music.common.models.datamodels.home

import dev.younesgouyd.apps.music.common.models.datamodels.*
import kotlinx.serialization.Serializable

@Serializable
data class HomeItems(
    val tracks: List<TrackRelation>,
    val playlists: List<Playlist>,
    val albums: List<SpotifyAlbum>,
    val artists: List<SpotifyArtist>,
    val tags: List<Tag>,
    val orderedFeed: List<HomeFeedStub>
)