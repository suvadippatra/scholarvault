package com.scholarvault.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
import java.io.InputStream

class SecurityVault(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "scholar_vault_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSecureString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getSecureString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun removeSecureValue(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun saveEncryptedFile(fileName: String, data: ByteArray): Result<File> {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            val encryptedFile = getEncryptedFile(file)
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(data)
                outputStream.flush()
            }
            Result.success(file)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    fun saveEncryptedFileFromStream(fileName: String, inputStream: InputStream): Result<File> {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            val encryptedFile = getEncryptedFile(file)
            encryptedFile.openFileOutput().use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }
            Result.success(file)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    fun getFileForViewing(originalFile: File, tempDir: File): File? {
        if (!originalFile.exists()) return null

        return try {
            val encryptedFile = getEncryptedFile(originalFile)
            val tempFile = File.createTempFile("decrypted_", ".tmp", tempDir)
            encryptedFile.openFileInput().use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            // Decryption failed. It might be unencrypted as fallback.
            originalFile
        }
    }

    fun readEncryptedFile(fileName: String): Result<ByteArray> {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                throw java.io.FileNotFoundException("File $fileName not found in internal storage")
            }
            val encryptedFile = getEncryptedFile(file)
            val data = encryptedFile.openFileInput().use { inputStream ->
                inputStream.readBytes()
            }
            Result.success(data)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }
    
    fun deleteEncryptedFile(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun getEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
}
