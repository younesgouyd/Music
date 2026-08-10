package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.SettingId

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
data class Setting(
    @PrimaryKey(autoGenerate = true)
    val id: SettingId,
    val name: String,
    val value: String,
    val creationDatetime: Long,
    val updateDatetime: Long
)