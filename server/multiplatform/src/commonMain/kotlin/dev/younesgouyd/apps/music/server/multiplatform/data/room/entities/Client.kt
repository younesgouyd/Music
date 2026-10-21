package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.DeviceType

@Entity
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: ClientId,
    val name: String,
    val deviceType: DeviceType?,
    val creationDatetime: Long,
    val updateDatetime: Long
)