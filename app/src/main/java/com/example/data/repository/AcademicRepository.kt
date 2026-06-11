package com.scholarvault.data.repository

import com.scholarvault.data.dao.AcademicItemDao
import com.scholarvault.data.model.AcademicItem
import com.scholarvault.data.model.CourseWithSemesters
import com.scholarvault.data.model.Semester
import kotlinx.coroutines.flow.Flow

class AcademicRepository(
    private val academicItemDao: AcademicItemDao
) {
    fun getAllCoursesWithSemesters(): Flow<List<CourseWithSemesters>> {
        return academicItemDao.getAllCoursesWithSemesters()
    }

    fun getAllAcademicItems(): Flow<List<AcademicItem>> {
        return academicItemDao.getAllAcademicItems()
    }

    fun getCourseWithSemestersById(id: String): Flow<CourseWithSemesters?> {
        return academicItemDao.getCourseWithSemestersById(id)
    }

    fun getLinksForAcademicItem(id: String): Flow<List<com.scholarvault.data.model.AcademicDocumentLink>> {
        return academicItemDao.getLinksForAcademicItem(id)
    }

    suspend fun insert(academicItem: AcademicItem) {
        academicItemDao.insert(academicItem)
    }

    suspend fun insertSemester(semester: Semester) {
        academicItemDao.insertSemester(semester)
    }

    suspend fun insertAcademicDocumentLink(link: com.scholarvault.data.model.AcademicDocumentLink) {
        academicItemDao.insertAcademicDocumentLink(link)
    }

    suspend fun deleteSemestersForCourse(courseId: String) {
        academicItemDao.deleteSemestersForCourse(courseId)
    }
}
