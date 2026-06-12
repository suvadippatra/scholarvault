package com.scholarvault.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scholarvault.MainApplication
import com.scholarvault.data.model.ProfileDocumentLink
import com.scholarvault.data.model.ProfileExperience
import com.scholarvault.data.model.ProfileWork
import com.scholarvault.data.model.UserProfile
import com.scholarvault.data.model.UserProfileWithDetails
import com.example.data.repository.SecureProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val secureProfileManager = SecureProfileManager(application)

    val profileStream: StateFlow<UserProfileWithDetails?> = secureProfileManager.profileStream

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveProfile(profile: UserProfile, experiences: List<ProfileExperience>, works: List<ProfileWork>, documents: List<ProfileDocumentLink> = emptyList()) {
        viewModelScope.launch {
            secureProfileManager.saveProfileData(profile, experiences, works, documents)
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            val current = profileStream.value
            secureProfileManager.saveProfileData(
                profile = profile,
                experiences = current?.experiences ?: emptyList(),
                works = current?.works ?: emptyList(),
                documents = current?.documents ?: emptyList()
            )
        }
    }

    fun addExperience(role: String, duration: String, location: String) {
        viewModelScope.launch {
            val current = profileStream.value
            val profile = current?.profile ?: UserProfile()
            val experiences = (current?.experiences ?: emptyList()) + ProfileExperience(
                id = UUID.randomUUID().toString(),
                role = role,
                duration = duration,
                location = location
            )
            secureProfileManager.saveProfileData(profile, experiences, current?.works ?: emptyList(), current?.documents ?: emptyList())
        }
    }
    
    fun removeExperience(exp: ProfileExperience) {
        viewModelScope.launch {
            val current = profileStream.value ?: return@launch
            val experiences = current.experiences.filter { it.id != exp.id }
            secureProfileManager.saveProfileData(current.profile, experiences, current.works, current.documents)
        }
    }

    fun addWork(title: String, date: String, isWebLink: Boolean) {
        viewModelScope.launch {
            val current = profileStream.value
            val profile = current?.profile ?: UserProfile()
            val works = (current?.works ?: emptyList()) + ProfileWork(
                id = UUID.randomUUID().toString(),
                title = title,
                date = date,
                isWebLink = isWebLink
            )
            secureProfileManager.saveProfileData(profile, current?.experiences ?: emptyList(), works, current?.documents ?: emptyList())
        }
    }
    
    fun removeWork(work: ProfileWork) {
        viewModelScope.launch {
            val current = profileStream.value ?: return@launch
            val works = current.works.filter { it.id != work.id }
            secureProfileManager.saveProfileData(current.profile, current.experiences, works, current.documents)
        }
    }

    fun addDocumentLink(fieldKey: String, walletCardId: String?, attachmentId: String?, displayName: String) {
        viewModelScope.launch {
            val current = profileStream.value
            val profile = current?.profile ?: UserProfile()
            val link = ProfileDocumentLink(
                id = UUID.randomUUID().toString(),
                fieldKey = fieldKey,
                walletCardId = walletCardId,
                attachmentId = attachmentId,
                displayName = displayName
            )
            val docs = (current?.documents ?: emptyList()).filter { it.fieldKey != fieldKey } + link
            secureProfileManager.saveProfileData(profile, current?.experiences ?: emptyList(), current?.works ?: emptyList(), docs)
        }
    }

    fun removeDocumentLink(fieldKey: String) {
        viewModelScope.launch {
            val current = profileStream.value ?: return@launch
            val docs = current.documents.filter { it.fieldKey != fieldKey }
            secureProfileManager.saveProfileData(current.profile, current.experiences, current.works, docs)
        }
    }
}
