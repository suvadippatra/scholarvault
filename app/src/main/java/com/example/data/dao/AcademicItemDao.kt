package com.scholarvault.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.scholarvault.data.model.AcademicItem
import com.scholarvault.data.model.CourseWithSemesters
import com.scholarvault.data.model.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicItemDao {
    @Transaction
    @Query("SELECT * FROM academic_items ORDER BY timeline_date DESC")
    fun getAllCoursesWithSemesters(): Flow<List<CourseWithSemesters>>

    @Transaction
    @Query("SELECT * FROM academic_items WHERE id = :id LIMIT 1")
    fun getCourseWithSemestersById(id: String): Flow<CourseWithSemesters?>

    @Query("SELECT * FROM academic_items ORDER BY timeline_date ASC")
    fun getAllAcademicItems(): Flow<List<AcademicItem>>

    @Query("SELECT * FROM academic_items WHERE type = :type ORDER BY timeline_date DESC")
    fun getAcademicItemsByType(type: String): Flow<List<AcademicItem>>

    @Query("SELECT * FROM academic_items WHERE id = :id LIMIT 1")
    suspend fun getAcademicItemById(id: String): AcademicItem?

    @Query("SELECT * FROM academic_items WHERE title LIKE '%' || :searchQuery || '%' ORDER BY timeline_date DESC")
    fun searchAcademicItems(searchQuery: String): Flow<List<AcademicItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(academicItem: AcademicItem)

    @Update
    suspend fun update(academicItem: AcademicItem)

    @Delete
    suspend fun delete(academicItem: AcademicItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicDocumentLink(link: com.scholarvault.data.model.AcademicDocumentLink)
    
    @Query("SELECT * FROM academic_document_links WHERE academic_item_id = :itemId")
    fun getLinksForAcademicItem(itemId: String): Flow<List<com.scholarvault.data.model.AcademicDocumentLink>>

    @Query("DELETE FROM semesters WHERE course_id = :courseId")
    suspend fun deleteSemestersForCourse(courseId: String)
}
