package dev.younesgouyd.libs.music.spotifyapi.repoes

import dev.younesgouyd.libs.music.spotifyapi.models.SearchResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

internal class SearchRepo(
    private val client: HttpClient,
    private val serializer: Json,
    private val getToken: suspend () -> String
) {
    /**
     * GET /search
     * @param query Your search query.
     * You can narrow down your search using field filters. The available filters are album, artist, track, year, upc, tag:hipster, tag:new, isrc, and genre. Each field filter only applies to certain result types.
     * The artist and year filters can be used while searching albums, artists and tracks. You can filter on a single year or a range (e.g. 1955-1960).
     * The album filter can be used while searching albums and tracks.
     * The genre filter can be used while searching artists and tracks.
     * The isrc and track filters can be used while searching tracks.
     * The upc, tag:new and tag:hipster filters can only be used while searching albums. The tag:new filter will return albums released in the past two weeks and tag:hipster can be used to return only albums with the lowest 10% popularity.
     * Example: q=remaster%2520track%3ADoxy%2520artist%3AMiles%2520Davis
     */
    suspend fun search(track: String, artist: String?, album: String?, year: String?): SearchResult {
        require(track.isNotBlank())
        val response = client.get("search") {
            header("Authorization", "Bearer ${getToken()}")
            parameter(
                key = "q",
                value = buildString {
                    append("track:$track")
                    if (artist?.isNotBlank() == true) { append(" artist:$artist") }
                    if (album?.isNotBlank() == true) { append(" album:$album") }
                    if (year?.isNotBlank() == true) { append(" year:$year") }
                }
            )
            parameter("type", "track")
            parameter("limit", 50)
            parameter("offset", 0)
        }.bodyAsText()
        return serializer.decodeFromString<SearchResult>(response)
    }

}