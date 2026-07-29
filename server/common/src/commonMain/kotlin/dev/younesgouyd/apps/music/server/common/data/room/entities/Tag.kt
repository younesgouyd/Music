package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.TagId

@Entity
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)