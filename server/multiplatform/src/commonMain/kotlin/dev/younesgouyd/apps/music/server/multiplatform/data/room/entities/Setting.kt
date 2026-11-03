package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
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