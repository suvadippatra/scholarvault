package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Date = Date(),
    val colorHex: String = "#FFF59D",
    val tags: List<String> = emptyList(),
    val folder: String? = null
)
