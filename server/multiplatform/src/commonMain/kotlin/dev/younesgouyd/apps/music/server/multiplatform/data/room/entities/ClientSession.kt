package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ClientSessionId

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
        Index(value = ["clientId"])
    ]
)
data class ClientSession(
    @PrimaryKey(autoGenerate = true)
    val id: ClientSessionId,
    val clientId: ClientId,
    val connectionDatetime: Long,
    val disconnectionDatetime: Long?,
    val disconnectionFlag: Boolean,
    val creationDatetime: Long,
    val updateDatetime: Long
)