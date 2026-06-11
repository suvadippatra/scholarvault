package com.scholarvault.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "semesters",
    foreignKeys = [
        ForeignKey(
            entity = AcademicItem::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["course_id"])
    ]
)
data class Semester(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "course_id") val courseId: String,
    @ColumnInfo(name = "semester_number") val semesterNumber: Int,
    @ColumnInfo(name = "semester_label") val semesterLabel: String = "",
    @ColumnInfo(name = "subjects_json") val subjectsJson: String = "[]",
    @ColumnInfo(name = "total_credits") val totalCredits: Double = 0.0,
    @ColumnInfo(name = "earned_sgpa") val earnedSgpa: Double? = null,
    val data: String = "",
    @ColumnInfo(name = "roll_number") val rollNumber: String? = null,
    @ColumnInfo(name = "override_roll_number") val overrideRollNumber: Boolean = false,
    @ColumnInfo(name = "attached_document_id") val attachedDocumentId: String? = null,
    @ColumnInfo(name = "attached_document_name") val attachedDocumentName: String? = null
)
