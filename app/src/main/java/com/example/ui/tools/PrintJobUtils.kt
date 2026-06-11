package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrintJobUtils {

    suspend fun createPrintJobItem(context: Context, uri: Uri): PrintJobItem = withContext(Dispatchers.IO) {
        val name = getFileName(context, uri)
        val type = context.contentResolver.getType(uri) ?: ""
        val isImage = type.startsWith("image/")
        
        var resolution = ""
        var dpi = 72
        
        if (isImage) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    resolution = "${options.outWidth}x${options.outHeight}"
                    if (options.inDensity > 0) {
                        dpi = options.inDensity
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (resolution.isBlank() || resolution == "-1x-1") {
                resolution = "Unknown"
            }
        }
        
        PrintJobItem(
            uri = uri,
            name = name,
            isImage = isImage,
            resolution = resolution,
            originalDpi = dpi,
            outputDpi = 300 // default target output DPI
        )
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown"
    }
}
