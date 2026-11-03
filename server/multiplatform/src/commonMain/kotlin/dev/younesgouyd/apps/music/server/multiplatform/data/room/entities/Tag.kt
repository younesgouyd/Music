package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.TagId

@Entity
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)