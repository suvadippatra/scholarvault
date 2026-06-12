package com.example.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.scholarvault.data.model.ProfileDocumentLink
import com.scholarvault.data.model.ProfileExperience
import com.scholarvault.data.model.ProfileWork
import com.scholarvault.data.model.UserProfile
import com.scholarvault.data.model.UserProfileWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SecureProfileManager(private val context: Context) {

    private val _profileFlow = MutableStateFlow<UserProfileWithDetails?>(null)
    val profileStream: StateFlow<UserProfileWithDetails?> = _profileFlow.asStateFlow()

    private val fileName = "userProfileData.json"
    private val gson = Gson()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private fun getEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            _profileFlow.value = null
            return
        }

        try {
            val encryptedFile = getEncryptedFile(file)
            val inputStream = encryptedFile.openFileInput()
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            if (jsonString.isNotEmpty()) {
                val details = gson.fromJson(jsonString, UserProfileWithDetails::class.java)
                _profileFlow.value = details
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (e.g. key invalidated), delete the file and reset stream
            if (file.exists()) file.delete()
            _profileFlow.value = null
        }
    }

    fun saveProfileData(
        profile: UserProfile,
        experiences: List<ProfileExperience>,
        works: List<ProfileWork>,
        documents: List<ProfileDocumentLink> = emptyList()
    ) {
        val details = UserProfileWithDetails(profile, experiences, works, documents)
        val jsonString = gson.toJson(details)

        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }

        try {
            val encryptedFile = getEncryptedFile(file)
            val outputStream = encryptedFile.openFileOutput()
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            outputStream.close()
            
            _profileFlow.value = details
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearProfileData() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        _profileFlow.value = null
    }
}
