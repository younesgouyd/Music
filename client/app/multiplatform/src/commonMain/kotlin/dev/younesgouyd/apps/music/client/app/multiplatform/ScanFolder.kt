package dev.younesgouyd.apps.music.client.app.multiplatform

import com.mpatric.mp3agic.Mp3File
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.io.File
import kotlin.io.encoding.Base64

typealias FileUri = String

expect suspend fun scanFolder(uri: FileUri): List<Inspection.ItemInspection.LocalFileTrack>

suspend fun scanMetadata(file: File, uri: String, path: List<String>): Inspection.ItemInspection.LocalFileTrack {
    var title: String? = null
    var durationMilliseconds: Long? = null
    var albumTrackNumber: Int? = null
    var artist: String? = null
    var album: String? = null
    var lyrics: String? = null
    var year: Int? = null
    var albumImage: ByteArray? = null
    withContext(Dispatchers.IO) {
        val mp3file = Mp3File(file)
        durationMilliseconds = mp3file.lengthInMilliseconds
        if (mp3file.hasId3v2Tag()) {
            val id3 = mp3file.id3v2Tag
            val albumImageData = id3.albumImage
            title = id3.title
            albumTrackNumber = id3.track.toNullIfBlank()?.toIntOrThrow()
            artist = id3.artist
            album = id3.album
            year = id3.year.toNullIfBlank()?.toIntOrThrow()
            lyrics = id3.lyrics
            albumImage = albumImageData
        } else if (mp3file.hasId3v1Tag()) {
            val id3 = mp3file.id3v1Tag
            title = id3.title
            albumTrackNumber = id3.track.toNullIfBlank()?.toIntOrThrow()
            artist = id3.artist
            album = id3.album
            year = id3.year.toNullIfBlank()?.toIntOrThrow()
        }
    }
    if (durationMilliseconds == null) {
        TODO()
    }
    return Inspection.ItemInspection.LocalFileTrack(
        uri = uri,
        title = title.toNullIfBlank() ?: file.name,
        durationMilliseconds = durationMilliseconds,
        albumTrackNumber = albumTrackNumber,
        artists = artist.toNullIfBlank()?.split(";") ?: emptyList(),
        album = album.toNullIfBlank(),
        path = path,
        lyrics = lyrics.toNullIfBlank(),
        year = year,
        albumImage = albumImage?.let { Base64.encode(it) }
    )
}

private fun String?.toNullIfBlank(): String? {
    if (this == null) return null
    if (this.isBlank()) return null
    return this
}

private fun String.toIntOrThrow(): Int {
    val value = this.toIntOrNull()
    if (value != null) {
        return value
    }
    TODO()
}

private val tika = Tika()

fun File.isAudioFile(): Boolean {
    val mimeType = tika.detect(this)
    return mimeType in audioMimeTypes
}

private val audioMimeTypes = setOf(
    // Uncompressed / PCM
    "audio/wav",
    "audio/x-wav", // (legacy)
    "audio/basic", //(µ-law, a-law)

    // MPEG family
    "audio/mpeg", // MP3
    "audio/aac", // audio-only MP4, AAC/ALAC
    "audio/mp4", // AAC in 3GPP container
    "audio/3gpp",
    "audio/3gpp2",

    // Ogg / Opus / Vorbis
    "application/ogg", // container, official default
    "audio/ogg", // when audio-only
    "audio/opus",
    "audio/vorbis",

    // FLAC / Lossless
    "audio/flac",
    "audio/alac", // less common, Apple Lossless

    // RealAudio
    "audio/vnd.rn-realaudio",

    // Windows formats
    "audio/x-ms-wma",
    "audio/x-ms-wax",

    // Other common ones
    "audio/amr",
    "audio/amr-wb",
    "audio/midi",
    "audio/x-midi",
    "audio/x-aiff",

    // Special cases
    "audio/webm",
    "application/x-shockwave-flash"
)