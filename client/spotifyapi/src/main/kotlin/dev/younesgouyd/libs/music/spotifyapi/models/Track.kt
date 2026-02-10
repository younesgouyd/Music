package dev.younesgouyd.libs.music.spotifyapi.models

import dev.younesgouyd.libs.music.spotifyapi.models.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /tracks/{id}
 */
@Serializable
data class Track(
    val album: Album,
    val artists: List<SimplifiedArtistObject>,
    @SerialName("disc_number")
    val discNumber: Int? = null,
    @SerialName("duration_ms")
    val durationMs: Int? = null,
    val explicit: Boolean? = null,
    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null,
    val href: String? = null,
    val id: TrackId,
    @SerialName("is_playable")
    val isPlayable: Boolean? = null,
    val restrictions: Restrictions? = null,
    val name: String,
    @SerialName("track_number")
    val trackNumber: Int? = null,
    val type: String? = null,
    val uri: SpotifyUri? = null,
    val isLocal: Boolean? = null
) {
    @Serializable
    data class Album(
        @SerialName("album_type")
        val albumType: String,
        @SerialName("total_tracks")
        val totalTracks: Int,
        @SerialName("external_urls")
        val externalUrls: ExternalUrls,
        val href: String,
        val id: AlbumId,
        val images: List<ImageObject>? = null,
        val name: String,
        @SerialName("release_date")
        val releaseDate: String,
        @SerialName("release_date_precision")
        val releaseDatePrecision: String,
        val restrictions: Restrictions? = null,
        val type: String,
        val uri: SpotifyUri,
        val artists: List<SimplifiedArtistObject>
    )
}