package com.scholarvault.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LockType { NONE, PIN, PASSWORD, PATTERN }

class WalletSecurityManager(context: Context) {

    var isSessionUnlocked = false

    private val PREFS_NAME = "wallet_security_prefs"
    private val KEY_SECURITY_ENABLED = "security_enabled"
    private val KEY_LOCK_TYPE = "lock_type"
    private val KEY_LOCK_DATA = "lock_data"
    private val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private val KEY_SEC_Q = "security_question"
    private val KEY_SEC_A = "security_answer"

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("WalletSecurity", "EncryptedPrefs failed — attempting cleanup and retry", e)
        try {
            // Delete corrupt shared preferences file to allow recreating keys
            context.deleteSharedPreferences(PREFS_NAME)
        } catch (ex: Exception) {
            Log.e("WalletSecurity", "Failed to delete shared prefs file", ex)
        }
        try {
            val masterKey2 = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey2,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e2: Exception) {
            Log.e("WalletSecurity", "EncryptedPrefs second attempt failed — falling back to standard SharedPreferences", e2)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _isSecurityEnabled = MutableStateFlow(sharedPreferences.getBoolean(KEY_SECURITY_ENABLED, false))
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled
    
    private val _lockType = MutableStateFlow(
        LockType.valueOf(sharedPreferences.getString(KEY_LOCK_TYPE, LockType.NONE.name) ?: LockType.NONE.name)
    )
    val lockType: StateFlow<LockType> = _lockType

    private val _isBiometricEnabled = MutableStateFlow(sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    fun setSecurity(type: LockType, data: String, question: String, answer: String, bioEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SECURITY_ENABLED, true)
            .putString(KEY_LOCK_TYPE, type.name)
            .putString(KEY_LOCK_DATA, data)
            .putString(KEY_SEC_Q, question)
            .putString(KEY_SEC_A, answer)
            .putBoolean(KEY_BIOMETRIC_ENABLED, bioEnabled)
            .apply()
        _isSecurityEnabled.value = true
        _lockType.value = type
        _isBiometricEnabled.value = bioEnabled
    }

    fun disableSecurity() {
        sharedPreferences.edit()
            .putBoolean(KEY_SECURITY_ENABLED, false)
            .putString(KEY_LOCK_TYPE, LockType.NONE.name)
            .remove(KEY_LOCK_DATA)
            .remove(KEY_SEC_Q)
            .remove(KEY_SEC_A)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        _isSecurityEnabled.value = false
        _lockType.value = LockType.NONE
        _isBiometricEnabled.value = false
        isSessionUnlocked = false
    }

    fun verifyLockData(data: String): Boolean {
        return sharedPreferences.getString(KEY_LOCK_DATA, null) == data
    }
    
    fun getSecurityQuestion(): String? {
        return sharedPreferences.getString(KEY_SEC_Q, null)
    }
    
    fun verifySecurityAnswer(answer: String): Boolean {
        // Simple case insensitive check
        val storedAnswer = sharedPreferences.getString(KEY_SEC_A, null)
        return storedAnswer?.trim()?.equals(answer.trim(), ignoreCase = true) == true
    }
    
    fun updateBiometric(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }
    
    fun updateLockData(data: String) {
        sharedPreferences.edit().putString(KEY_LOCK_DATA, data).apply()
    }
    
    fun updateSecurityQuestion(question: String, answer: String) {
        sharedPreferences.edit().putString(KEY_SEC_Q, question).putString(KEY_SEC_A, answer).apply()
    }
}
