package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TagId

@Entity
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: TagId,
    val name: String,
    val creationDatetime: Long
)