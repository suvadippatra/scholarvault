package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateTimeStr: String,
    val timeInMillis: Long,
    val isDeleted: Boolean = false
)
