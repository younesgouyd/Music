package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.client.data.SettingId

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