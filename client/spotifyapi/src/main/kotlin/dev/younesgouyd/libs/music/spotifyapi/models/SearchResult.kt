package dev.younesgouyd.libs.music.spotifyapi.models

import kotlinx.serialization.Serializable

/**
 * GET /search
 */
@Serializable
data class SearchResult(
    val tracks: Tracks? = null
) {
    @Serializable
    data class Tracks(
        val href: String,
        val limit: Int,
        val next: String? = null,
        val offset: Int,
        val previous: String? = null,
        val total: Int,
        val items: List<Track>
    )
}