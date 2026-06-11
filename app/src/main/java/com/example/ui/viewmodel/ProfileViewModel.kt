package com.scholarvault.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scholarvault.MainApplication
import com.scholarvault.data.model.ProfileExperience
import com.scholarvault.data.model.ProfileWork
import com.scholarvault.data.model.UserProfile
import com.scholarvault.data.model.UserProfileWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val profileDao = (application as MainApplication).database.profileDao()

    val profileStream: StateFlow<UserProfileWithDetails?> = profileDao.getUserProfileWithDetails()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveProfile(profile: UserProfile, experiences: List<ProfileExperience>, works: List<ProfileWork>) {
        viewModelScope.launch {
            profileDao.insertProfile(profile)
            experiences.forEach { profileDao.insertExperience(it) }
            works.forEach { profileDao.insertWork(it) }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            profileDao.insertProfile(profile)
        }
    }

    fun addExperience(role: String, duration: String, location: String) {
        viewModelScope.launch {
            val currentProfile = profileStream.value?.profile ?: UserProfile()
            profileDao.insertProfile(currentProfile)
            profileDao.insertExperience(
                ProfileExperience(
                    id = UUID.randomUUID().toString(),
                    role = role,
                    duration = duration,
                    location = location
                )
            )
        }
    }
    
    fun removeExperience(exp: ProfileExperience) {
        viewModelScope.launch {
            profileDao.deleteExperience(exp)
        }
    }

    fun addWork(title: String, date: String, isWebLink: Boolean) {
        viewModelScope.launch {
            val currentProfile = profileStream.value?.profile ?: UserProfile()
            profileDao.insertProfile(currentProfile)
            profileDao.insertWork(
                ProfileWork(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    date = date,
                    isWebLink = isWebLink
                )
            )
        }
    }
    
    fun removeWork(work: ProfileWork) {
        viewModelScope.launch {
            profileDao.deleteWork(work)
        }
    }
}
