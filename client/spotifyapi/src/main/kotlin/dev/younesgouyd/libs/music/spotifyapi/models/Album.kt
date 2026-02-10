package dev.younesgouyd.libs.music.spotifyapi.models

import dev.younesgouyd.libs.music.spotifyapi.models.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /albums/{id}
 */
@Serializable
data class Album(
    @SerialName("album_type")
    val albumType: String,
    @SerialName("total_tracks")
    val totalTracks: Int,
    @SerialName("available_markets")
    val availableMarkets: List<String>,
    @SerialName("external_urls")
    val externalUrls: ExternalUrls,
    val href: String,
    val id: AlbumId,
    val images: List<ImageObject>,
    val name: String,
    @SerialName("release_date")
    val releaseDate: String,
    @SerialName("release_date_precision")
    val releaseDatePrecision: String,
    val restrictions: Restrictions? = null,
    val type: String,
    val uri: SpotifyUri,
    val artists: List<SimplifiedArtistObject>,
    val tracks: Tracks,
    val copyrights: List<CopyrightObject>
) {
    @Serializable
    data class Tracks(
        val href: String,
        val limit: Int,
        val next: String? = null,
        val offset: Int,
        val previous: String? = null,
        val total: Int,
        val items: List<SimplifiedTrackObject>
    ) {
        @Serializable
        data class SimplifiedTrackObject(
            val artists: List<SimplifiedArtistObject>? = null,
            @SerialName("disc_number")
            val discNumber: Int? = null,
            @SerialName("duration_ms")
            val durationMs: Int? = null,
            val explicit: Boolean? = null,
            @SerialName("external_urls")
            val externalUrls: ExternalUrls? = null,
            val href: String? = null,
            val id: TrackId? = null,
            @SerialName("is_playable")
            val isPlayable: Boolean? = null,
            val restrictions: Restrictions? = null,
            val name: String? = null,
            @SerialName("track_number")
            val trackNumber: Int? = null,
            val type: String? = null,
            val uri: SpotifyUri? = null,
            @SerialName("is_local")
            val isLocal: Boolean? = null
        )
    }

    @Serializable
    data class CopyrightObject(
        val text: String? = null,
        val type: String? = null
    )
}
