package com.scholarvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scholarvault.data.model.AcademicItem
import com.scholarvault.data.model.CourseWithSemesters
import com.scholarvault.data.repository.AcademicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AcademicViewModel(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val coursesWithSemesters: StateFlow<List<CourseWithSemesters>> = repository.getAllCoursesWithSemesters()
        .catch { e ->
            _errorState.value = "Failed to load courses: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val academicItems: StateFlow<List<AcademicItem>> = repository.getAllAcademicItems()
        .catch { e ->
            _errorState.value = "Failed to load items: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertAcademicItem(academicItem: AcademicItem) {
        viewModelScope.launch {
            try {
                repository.insert(academicItem)
                _errorState.value = null 
            } catch (e: Exception) {
                _errorState.value = "Failed to save record: ${e.message}"
            }
        }
    }

    fun getCourseWithSemesters(id: String): StateFlow<CourseWithSemesters?> {
        return repository.getCourseWithSemestersById(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun getLinksForAcademicItem(id: String): StateFlow<List<com.scholarvault.data.model.AcademicDocumentLink>> {
        return repository.getLinksForAcademicItem(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insertSemester(semester: com.scholarvault.data.model.Semester) {
        viewModelScope.launch {
            try {
                repository.insertSemester(semester)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = "Failed to save semester: ${e.message}"
            }
        }
    }

    fun updateSemester(semester: com.scholarvault.data.model.Semester) {
        viewModelScope.launch {
            try {
                repository.insertSemester(semester) // insert with same ID should replace in Room if configured, or I should use update.
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = "Failed to update semester: ${e.message}"
            }
        }
    }

    fun insertAcademicDocumentLink(link: com.scholarvault.data.model.AcademicDocumentLink) {
        viewModelScope.launch {
            try {
                repository.insertAcademicDocumentLink(link)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = "Failed to save document link: ${e.message}"
            }
        }
    }

    fun deleteSemestersForCourse(courseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSemestersForCourse(courseId)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = "Failed to clear semesters: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    class Factory(private val repository: AcademicRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AcademicViewModel::class.java)) {
                return AcademicViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
