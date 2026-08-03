package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["smallImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["mediumImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["largeImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["spotifyId"], unique = true),
        Index(value = ["smallImgId"], unique = true),
        Index(value = ["mediumImgId"], unique = true),
        Index(value = ["largeImgId"], unique = true)
    ]
)
data class SpotifyArtist(
    @PrimaryKey
    val id: SpotifyArtistId,
    val spotifyId: String,
    val name: String,
    val smallImgId: MediaFileId?,
    val mediumImgId: MediaFileId?,
    val largeImgId: MediaFileId?,
    val apiResponse: String,
    val creationDatetime: Long
)