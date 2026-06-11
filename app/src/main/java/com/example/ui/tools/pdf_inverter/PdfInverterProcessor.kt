package com.scholarvault.ui.tools.pdf_inverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfInverterProcessor {
    private val mutex = Mutex()

    fun parsePages(pagesStr: String, maxPages: Int): Set<Int> {
        if (pagesStr.trim().equals("all", ignoreCase = true) || pagesStr.trim().isEmpty()) {
            return (0 until maxPages).toSet()
        }
        val result = mutableSetOf<Int>()
        val parts = pagesStr.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.isEmpty()) continue
            if (p.contains("-")) {
                val rangeParts = p.split("-")
                val start = rangeParts[0].toIntOrNull() ?: 1
                val end = rangeParts.getOrNull(1)?.toIntOrNull() ?: maxPages
                val realStart = (start - 1).coerceIn(0, maxPages - 1)
                val realEnd = (end - 1).coerceIn(0, maxPages - 1)
                for (i in realStart..realEnd) {
                    result.add(i)
                }
            } else {
                val num = p.toIntOrNull()
                if (num != null) {
                    result.add((num - 1).coerceIn(0, maxPages - 1))
                }
            }
        }
        return result
    }

    suspend fun processPdfInversion(
        context: Context, 
        item: PdfItem,
        onProgress: suspend (Int) -> Unit = {}
    ): Uri? = withContext(Dispatchers.IO) {
        mutex.withLock {
            var pfd: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            var pdfDocument: PdfDocument? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(item.uri, "r") ?: return@withLock null
                pdfRenderer = PdfRenderer(pfd)
                
                val maxPages = pdfRenderer.pageCount
                val pagesToInvert = parsePages(item.pagesToInvertStr, maxPages)
                
                pdfDocument = PdfDocument()
                
                val paint = Paint().apply {
                    colorFilter = when (item.mode) {
                        InvertMode.WORD_PDF -> {
                            ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                                -1f,  0f,  0f,  0f, 255f,
                                 0f, -1f,  0f,  0f, 255f,
                                 0f,  0f, -1f,  0f, 255f,
                                 0f,  0f,  0f,  1f,   0f
                            )))
                        }
                        InvertMode.SCANNED_PDF -> {
                            val matrix = ColorMatrix(floatArrayOf(
                                -1.2f,  0f,  0f,  0f, 255f,
                                 0f, -1.2f,  0f,  0f, 255f,
                                 0f,  0f, -1.2f,  0f, 255f,
                                 0f,  0f,  0f,  1f,   0f
                            ))
                            ColorMatrixColorFilter(matrix)
                        }
                        InvertMode.SMART_INVERT -> {
                            ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                                -1f, 0f, 0f, 0f, 255f,
                                0f, -1f, 0f, 0f, 255f,
                                0f, 0f, -1f, 0f, 255f,
                                0f,  0f,  0f, 1f,   0f
                            )))
                        }
                    }
                }
                
                for (i in 0 until maxPages) {
                    var bitmap: Bitmap? = null
                    try {
                        kotlinx.coroutines.yield()
                        val page = pdfRenderer.openPage(i)
                        // High DPI for processing, matching standard screen / 2x
                        val width = (page.width * 2.0).toInt()
                        val height = (page.height * 2.0).toInt()
                        
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        
                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, i).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        
                        if (pagesToInvert.contains(i)) {
                            newPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        } else {
                            newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        }
                        
                        pdfDocument.finishPage(newPage)
                        
                        val progress = ((i + 1) * 100) / maxPages
                        onProgress(progress)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } finally {
                        bitmap?.recycle()
                    }
                }
                
                var outName = item.newName
                if (!outName.lowercase().endsWith(".pdf")) outName += ".pdf"
                
                val tempDir = File(context.cacheDir, "pdf_inverter_temp").apply { mkdirs() }
                val tempOutFile = File(tempDir, "inverted_${UUID.randomUUID()}.pdf")
                FileOutputStream(tempOutFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempOutFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                pdfDocument?.close()
                pdfRenderer?.close()
                pfd?.close()
            }
        }
    }

    companion object {
        fun parsePages(pagesStr: String, maxPages: Int): Set<Int> {
            if (pagesStr.trim().equals("all", ignoreCase = true) || pagesStr.trim().isEmpty()) {
                return (0 until maxPages).toSet()
            }
            val result = mutableSetOf<Int>()
            val parts = pagesStr.split(",")
            for (part in parts) {
                val p = part.trim()
                if (p.isEmpty()) continue
                if (p.contains("-")) {
                    val rangeParts = p.split("-")
                    val start = rangeParts[0].toIntOrNull() ?: 1
                    val end = rangeParts.getOrNull(1)?.toIntOrNull() ?: maxPages
                    val realStart = (start - 1).coerceIn(0, maxPages - 1)
                    val realEnd = (end - 1).coerceIn(0, maxPages - 1)
                    for (i in realStart..realEnd) {
                        result.add(i)
                    }
                } else {
                    val num = p.toIntOrNull()
                    if (num != null) {
                        result.add((num - 1).coerceIn(0, maxPages - 1))
                    }
                }
            }
            return result
        }

        suspend fun saveItemToDownloads(context: Context, sourceUri: Uri, desiredFileName: String): Uri? = withContext(Dispatchers.IO) {
            try {
                var outName = desiredFileName.trim()
                if (outName.isEmpty()) outName = "Inverted_Document.pdf"
                if (!outName.lowercase().endsWith(".pdf")) outName += ".pdf"
                
                // Clean up name from directory traversal characters
                outName = outName.replace("/", "_").replace("\\", "_")

                var finalUri: Uri? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, outName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/ScholarVault/PdfColorInverter")
                    }
                    finalUri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    finalUri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            context.contentResolver.openInputStream(sourceUri)?.use { input ->
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
                    val destDir = File(downloadDir, "ScholarVault/PdfColorInverter")
                    if (!destDir.exists()) destDir.mkdirs()
                    
                    var outFile = File(destDir, outName)
                    var counter = 1
                    while (outFile.exists()) {
                        val base = outName.substringBeforeLast(".")
                        val ext = outName.substringAfterLast(".", "pdf")
                        outFile = File(destDir, "${base}_($counter).$ext")
                        counter++
                    }
                    
                    FileOutputStream(outFile).use { out ->
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            input.copyTo(out)
                        }
                    }
                    finalUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                }
                finalUri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        suspend fun saveItemToAppFolder(
            context: Context,
            sourceUri: Uri,
            desiredFileName: String,
            tagsCommaSeparated: String,
            repository: com.scholarvault.data.repository.DocumentRepository
        ): Boolean = withContext(Dispatchers.IO) {
            try {
                var outName = desiredFileName.trim()
                if (outName.isEmpty()) outName = "Inverted_Document.pdf"
                if (!outName.lowercase().endsWith(".pdf")) outName += ".pdf"
                
                outName = outName.replace("/", "_").replace("\\", "_")
                
                val vaultFileName = "${System.currentTimeMillis()}_$outName"
                val vault = com.scholarvault.util.SecurityVault(context)
                var sizeBytes = 0L
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val tempFile = File(context.cacheDir, "inverter_tmp_${System.currentTimeMillis()}")
                    try {
                        FileOutputStream(tempFile).use { tempOut ->
                            sizeBytes = input.copyTo(tempOut)
                        }
                        java.io.FileInputStream(tempFile).use { tempIn ->
                            vault.saveEncryptedFileFromStream(vaultFileName, tempIn)
                        }
                    } finally {
                        if (tempFile.exists()) tempFile.delete()
                    }
                }
                val sandboxedFile = File(context.filesDir, vaultFileName)
                
                // Parse tags
                val tagList = tagsCommaSeparated.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                val docFile = com.scholarvault.data.model.DocumentFile(
                    name = outName,
                    isFolder = false,
                    parentFolderId = null,
                    extension = "pdf",
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

        fun pruneOldFiles(context: Context) {
            val outputDir = File(context.cacheDir, "pdf_inverter_temp")
            if (outputDir.exists()) {
                val files = outputDir.listFiles() ?: return
                val now = System.currentTimeMillis()
                val threeDaysInMillis = 3L * 24 * 60 * 60 * 1000
                files.forEach { file ->
                    if (now - file.lastModified() > threeDaysInMillis) {
                        file.delete()
                    }
                }
            }
        }
    }
}
