package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ClientSettingId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clientId", "name"], unique = true)
    ]
)
data class ClientSetting(
    @PrimaryKey(autoGenerate = true)
    val id: ClientSettingId,
    val clientId: ClientId,
    val name: String,
    val value: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)