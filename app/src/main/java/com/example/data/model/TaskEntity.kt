package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
    val isDeleted: Boolean = false,
    val completedAt: Long? = null
)
