package com.scholarvault.ui.tools.pdf_inverter

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HistoryItem(val uri: Uri, val name: String, val sizeBytes: Long, val lastModified: Long = 0L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfHistoryScreen(onBack: () -> Unit, onPreview: (Uri) -> Unit) {
    val context = LocalContext.current
    var historyFiles by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val itemsMap = mutableMapOf<String, HistoryItem>()
            val resolver = context.contentResolver
            val queryUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
            
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )
            
            val selection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.MediaColumns.DATA} LIKE ?"
            }
            
            val selectionArgs = arrayOf("%ScholarVault/PdfColorInverter%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            
            // 1. MediaStore query
            try {
                resolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val size = cursor.getLong(sizeCol)
                        val date = cursor.getLong(dateCol) * 1000L
                        val uri = ContentUris.withAppendedId(queryUri, id)
                        if (name.endsWith(".pdf", ignoreCase = true)) {
                            itemsMap[name] = HistoryItem(uri, name, size, date)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Direct public downloads directory fallback query
            try {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val folder = java.io.File(downloadDir, "ScholarVault/PdfColorInverter")
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles()?.filter { it.extension.lowercase(java.util.Locale.ROOT) == "pdf" }
                    files?.forEach { file ->
                        if (!itemsMap.containsKey(file.name)) {
                            val uri = Uri.fromFile(file)
                            itemsMap[file.name] = HistoryItem(uri, file.name, file.length(), file.lastModified())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Direct private downloads directory fallback query
            try {
                val folder = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "ScholarVault/PdfColorInverter")
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles()?.filter { it.extension.lowercase(java.util.Locale.ROOT) == "pdf" }
                    files?.forEach { file ->
                        if (!itemsMap.containsKey(file.name)) {
                            val uri = Uri.fromFile(file)
                            itemsMap[file.name] = HistoryItem(uri, file.name, file.length(), file.lastModified())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            historyFiles = itemsMap.values.sortedByDescending { it.lastModified }
        }
    }

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Previous Inverted PDFs",
                showProfileIcon = false,
                showSearchBar = false
            )
        }
    ) { padding ->
        if (historyFiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No previous files found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(historyFiles) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${item.sizeBytes / 1024} KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onPreview(item.uri) }) {
                                Icon(Icons.Default.Preview, "Preview")
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF"))
                            }) {
                                Icon(Icons.Default.Share, "Share")
                            }
                            IconButton(onClick = {
                                com.scholarvault.ui.tools.SharedData.pendingUris.value = listOf(item.uri)
                                com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = true
                            }) {
                                Icon(Icons.Default.Print, "Print")
                            }
                            IconButton(onClick = {
                                try {
                                    context.contentResolver.delete(item.uri, null, null)
                                    historyFiles = historyFiles.toMutableList().apply { removeAt(index) }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
