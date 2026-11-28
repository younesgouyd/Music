package dev.younesgouyd.apps.music.ytdlp

import dev.younesgouyd.apps.music.common.models.Base64Image
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection
import dev.younesgouyd.apps.music.ytdlp.Api.YtDlpModels.Thumbnail
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

object Api {
    private val logger = KotlinLogging.logger {}
    private val tempDir = File(System.getProperty("user.home"), "ytdlpserverdata").also { it.mkdir() }
    private val downloadDir = File(tempDir, "download").also { it.mkdir() }
    private val ytDlpOutput = File(tempDir, "ytdlp_stdout")
    private val ytDlpErrorOutput = File(tempDir, "ytdlp_stderr")
    private val ytDlpSerializer = Json { ignoreUnknownKeys = true }

    suspend fun inspect(url: String): Inspection {
        logger.debug { "--> ::inspect(url: $url)" }
        ensureLatestVersion()
        val containerCommand = runCommand(
            args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", url),
            ignoreError = true
        )
        val ytDlpType = ytDlpSerializer.decodeFromString<YtDlpModels.Type>(containerCommand)
        return when (ytDlpType.type) {
            "playlist" -> {
                val container = ytDlpSerializer.decodeFromString<YtDlpModels.Container>(containerCommand)
                val containerThumbnail = tryList(container.thumbnails.reversed())
                Inspection(
                    container = Inspection.Container(
                        uri = url,
                        title = container.title.ifBlank { TODO() },
                        description = container.description?.nullIfBlank(),
                        thumbnailUrl = containerThumbnail?.url,
                        thumbnail = containerThumbnail?.base64,
                    ),
                    items = buildList {
                        runCommand(listOf("--get-url", "--flat-playlist", "--simulate", "--quiet", "--no-warnings", url))
                            .split("\n")
                            .distinct()
                            .filter { it != "null" && it.isNotBlank() }
                            .forEachIndexed { index, itemUrl ->
                                val ytDlpInspection = runCommand(
                                    args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", itemUrl),
                                    ignoreError = true
                                )
                                if (ytDlpInspection.isNotBlank() && ytDlpInspection.trim() != "null") {
                                    val entry = ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(ytDlpInspection)
                                    val resolved = resolveEntryThumbnail(entry)
                                    val item = Inspection.Item(
                                        uri = entry.url.ifBlank { TODO() },
                                        title = entry.title.ifBlank { TODO() },
                                        durationMilliseconds = entry.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                                        artists = entry.artists,
                                        album = entry.album?.let { if (it == "null") null else it.nullIfBlank() },
                                        id = (index + 1).toLong(),
                                        thumbnailUrl = resolved?.url,
                                        thumbnail = resolved?.base64
                                    )
                                    add(item)
                                }
                            }
                    }
                )
            }
            else -> {
                val single = ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(containerCommand)
                val title = single.title.ifBlank { TODO() }
                val resolved = resolveEntryThumbnail(single)
                Inspection(
                    container = Inspection.Container(
                        uri = url,
                        title = title,
                        description = null,
                        thumbnailUrl = null,
                        thumbnail = null
                    ),
                    items = listOf(
                        Inspection.Item(
                            uri = single.url.ifBlank { TODO() },
                            title = title,
                            durationMilliseconds = single.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                            artists = single.artists,
                            album = single.album?.nullIfBlank(),
                            id = 1,
                            thumbnailUrl = resolved?.url,
                            thumbnail = resolved?.base64
                        )
                    )
                )
            }
        }
    }

    suspend fun download(url: String) {
        return withContext(Dispatchers.IO) {
            downloadDir.listFiles().orEmpty().forEach {
                it.delete()
            }
            val commandResponse = runCommand(
                listOf(
                    "--extract-audio",
                    "--output", "${downloadDir.absolutePath}/%(title)s.%(ext)s",
                    url
                )
            )
            logger.debug { "::download | command response for $url: \n$commandResponse" }
        }
    }

    fun getResult(): File {
        val downloads = downloadDir.listFiles().orEmpty()
        if (downloads.isEmpty() || downloads.size > 1) { TODO() }
        return downloads[0]
    }

    private suspend fun ensureLatestVersion() {
        withContext(Dispatchers.IO) {
            val version = runCommand(listOf("--version"))
            logger.debug { "yt-dlp version: $version" }
            logger.debug { "calling --update..." }
            runCommand(listOf("--update"))
            val stdout = ytDlpOutput.readText()
            val stderr = ytDlpErrorOutput.readText()
            logger.debug { "--update stdout: \n$stdout" }
            logger.debug { "--update stderr: \n$stderr" }
        }
    }

    private suspend fun runCommand(args: List<String>, ignoreError: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            logger.debug { "::runCommand | args: ${args.joinToString(" ")}" }
            val process = ProcessBuilder("yt-dlp", *args.toTypedArray())
                .redirectOutput(ytDlpOutput)
                .redirectError(ytDlpErrorOutput)
                .start()
            try {
                logger.debug { "::runCommand | waiting for command to finish..." }
                val exitCode = runInterruptible { process.waitFor() }
                logger.debug { "::runCommand | command finished with exitCode: $exitCode" }
                if (!ignoreError && exitCode != 0) {
                    val errorMsg = ytDlpErrorOutput.readText()
                    throw Exception("yt-dlp failed: $errorMsg")
                }
                ytDlpOutput.readText()
            } catch (e: CancellationException) {
                logger.debug { "::runCommand | coroutine cancelled, terminating yt-dlp..." }
                process.destroyForcibly()
                throw e
            }
        }
    }

    private suspend fun resolveEntryThumbnail(entry: YtDlpModels.Entry): ResolvedThumbnail? {
        val sizeTokenRegex = Regex("""w\d+-h\d+""")
        val filtered = entry.thumbnails.filter { it.url.contains(sizeTokenRegex) }.reversed()
        val filteredResult = if (filtered.isNotEmpty()) {
            val url = filtered.first().url
            tryDownloadThumbnail(url.replace(sizeTokenRegex, "w1425-h1425"))
                ?: tryDownloadThumbnail(url.replace(sizeTokenRegex, "w1200-h1200"))
                ?: tryDownloadThumbnail(url.replace(sizeTokenRegex, "w1080-h1080"))
                ?: tryList(filtered)
        } else null
        return filteredResult ?: entry.thumbnail?.let { tryDownloadThumbnail(it) }
            ?: tryList(entry.thumbnails.filterNot { it.url.contains(sizeTokenRegex) }.reversed())
    }

    private suspend fun tryList(list: List<Thumbnail>): ResolvedThumbnail? {
        val urls = list.map { it.url }
        for (url in urls.filterNot { it.contains("maxresdefault") }) {
            return tryDownloadThumbnail(url) ?: continue
        }
        for (url in urls.filter { it.contains("maxresdefault") }) {
            return tryDownloadThumbnail(url) ?: continue
        }
        return null
    }

    private suspend fun tryDownloadThumbnail(url: String): ResolvedThumbnail? {
        return try {
            ResolvedThumbnail(
                url = url,
                base64 = downloadThumbnail(url)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "::tryDownloadThumbnail | failed to download thumbnail from $url" }
            null
        }
    }

    private suspend fun downloadThumbnail(url: String): Base64Image {
        return withContext(Dispatchers.IO) {
            val bytes = URI(url).toURL().readBytes()
            Base64.encode(bytes)
        }
    }

    private fun String.nullIfBlank(): String? {
        return this.ifBlank { null }
    }

    private data class ResolvedThumbnail(
        val url: String,
        val base64: Base64Image
    )

    private object YtDlpModels {
        @Serializable
        data class Type(
            @SerialName("_type")
            val type: String
        )

        @Serializable
        data class Thumbnail(
            val url: String,
            val height: Int? = null,
            val width: Int? = null,
            val preference: Int? = null,
            val id: String? = null,
            val resolution: String? = null
        )

        @Serializable
        data class Entry(
            val id: String,
            val title: String,
            val thumbnail: String? = null,
            val thumbnails: List<Thumbnail> = emptyList(),
            val duration: Int? = null, // TODO: confirm if this is always in seconds
            @SerialName("original_url") // TODO
            val url: String,
            val album: String? = null,
            val artists: List<String> = emptyList(),
            @SerialName("release_date")
            val releaseDate: String? = null
        )

        @Serializable
        data class Container(
            val id: String,
            val title: String,
            val description: String?,
            val thumbnails: List<Thumbnail>
        )
    }
}