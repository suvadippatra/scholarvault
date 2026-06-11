package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_annotations",
    foreignKeys = [
        ForeignKey(
            entity = DocumentFile::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class PageAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val pageIndex: Int,
    val typedNote: String? = null,
    val handwrittenDrawingJson: String? = null, // Stores path segments for redraws
    val lastModified: Long = System.currentTimeMillis()
)
