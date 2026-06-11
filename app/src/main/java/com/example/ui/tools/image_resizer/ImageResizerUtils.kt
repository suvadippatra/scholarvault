package com.scholarvault.ui.tools.image_resizer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.FileProvider

suspend fun saveImageToAppFolder(
    context: Context,
    imageUri: Uri,
    desiredFileName: String,
    format: Bitmap.CompressFormat,
    tagsCommaSeparated: String,
    repository: com.scholarvault.data.repository.DocumentRepository
): Boolean = withContext(Dispatchers.IO) {
    try {
        var outName = desiredFileName.trim()
        if (outName.isEmpty()) outName = "resized_image"
        val ext = format.name.lowercase()
        if (!outName.lowercase().endsWith(".$ext")) outName += ".$ext"
        
        outName = outName.replace("/", "_").replace("\\", "_")
        
        val vaultFileName = "${System.currentTimeMillis()}_$outName"
        val vault = com.scholarvault.util.SecurityVault(context)
        var sizeBytes = 0L
        val result = context.contentResolver.openInputStream(imageUri)?.use { input ->
            val tempFile = File(context.cacheDir, "resizer_tmp_${System.currentTimeMillis()}")
            try {
                java.io.FileOutputStream(tempFile).use { tempOut ->
                    sizeBytes = input.copyTo(tempOut)
                }
                java.io.FileInputStream(tempFile).use { tempIn ->
                    vault.saveEncryptedFileFromStream(vaultFileName, tempIn)
                }
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
        if (result == null) throw Exception("Failed to open input stream")
        val sandboxedFile = File(context.filesDir, vaultFileName)
        
        val tagList = tagsCommaSeparated.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        val docFile = com.scholarvault.data.model.DocumentFile(
            name = outName,
            isFolder = false,
            parentFolderId = null,
            extension = ext,
            sizeBytes = sizeBytes,
            createdAt = java.util.Date(),
            filePath = sandboxedFile.absolutePath,
            isEncrypted = true,
            tags = tagList,
            isTrashed = false
        )
        
        repository.insertFile(docFile)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

suspend fun saveImageToDownloads(
    context: Context,
    imageUri: Uri,
    desiredFileName: String,
    format: Bitmap.CompressFormat
): Uri? = withContext(Dispatchers.IO) {
    try {
        var outName = desiredFileName.trim()
        if (outName.isEmpty()) outName = "resized_image"
        val ext = format.name.lowercase()
        if (!outName.lowercase().endsWith(".$ext")) outName += ".$ext"
        
        outName = outName.replace("/", "_").replace("\\", "_")

        var finalUri: Uri? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, outName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "image/$ext")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/ScholarVault/ResizedImages")
            }
            finalUri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            finalUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        input.copyTo(out)
                    }
                }
            }
        } else {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@withContext null
            }
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destDir = File(downloadDir, "ScholarVault/ResizedImages")
            if (!destDir.exists()) destDir.mkdirs()
            
            var outFile = File(destDir, outName)
            var counter = 1
            while (outFile.exists()) {
                val base = outName.substringBeforeLast(".")
                val extStr = outName.substringAfterLast(".", ext)
                outFile = File(destDir, "${base}_($counter).$extStr")
                counter++
            }
            
            java.io.FileOutputStream(outFile).use { out ->
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.copyTo(out)
                }
            }
            finalUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }
        finalUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
