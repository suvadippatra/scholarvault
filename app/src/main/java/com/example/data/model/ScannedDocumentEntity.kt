package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "scanned_documents")
data class ScannedDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pagePaths: String, // JSON array of file paths
    val timestamp: Date = Date(),
    val tags: List<String> = emptyList(),
    val isTrashed: Boolean = false
)
