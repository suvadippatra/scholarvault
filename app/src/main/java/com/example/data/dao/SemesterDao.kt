package com.scholarvault.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scholarvault.data.model.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters WHERE course_id = :courseId ORDER BY semester_number ASC")
    fun getSemestersForCourse(courseId: String): Flow<List<Semester>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(semester: Semester)

    @Update
    suspend fun update(semester: Semester)

    @Delete
    suspend fun delete(semester: Semester)
}
