package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
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