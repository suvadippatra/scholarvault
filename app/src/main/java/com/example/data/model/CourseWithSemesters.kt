package com.scholarvault.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class CourseWithSemesters(
    @Embedded val course: AcademicItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "course_id"
    )
    val semesters: List<Semester>
)
