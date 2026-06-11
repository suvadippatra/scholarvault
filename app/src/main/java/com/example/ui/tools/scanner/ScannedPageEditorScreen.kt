package com.scholarvault.ui.tools.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedPageEditorScreen(
    uri: Uri,
    initialFilter: String = "Original",
    onDismiss: () -> Unit,
    onSave: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filterMode by remember { mutableStateOf(initialFilter) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            val file = File(uri.path!!)
            if (file.exists()) {
                val opts = BitmapFactory.Options()
                opts.inPreferredConfig = Bitmap.Config.RGB_565
                currentBitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Page") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val newUri = saveEditedBitmap(context, currentBitmap, filterMode)
                                isProcessing = false
                                if (newUri != null) {
                                    onSave(newUri, filterMode)
                                } else {
                                    onDismiss()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                val filters = listOf("Original", "Magic Enhance", "Grayscale", "B&W Document")
                var expanded by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { expanded = true }) {
                        Text("Filter: $filterMode")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filters.forEach { f ->
                            DropdownMenuItem(text = { Text(f) }, onClick = { 
                                filterMode = f
                                expanded = false 
                            })
                        }
                    }
                }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { /* MOCK: In a real app this opens 4-point crop */ }) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            currentBitmap?.let { bmp ->
                val displayBitmap = remember(bmp, filterMode) {
                    if (filterMode == "Original") bmp
                    else FilterEngine.applyFilter(bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true), filterMode)
                }
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Editing Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private suspend fun saveEditedBitmap(context: android.content.Context, bitmap: Bitmap?, filterMode: String): Uri? = withContext(Dispatchers.IO) {
    if (bitmap == null) return@withContext null
    try {
        val filtered = if (filterMode == "Original") bitmap else FilterEngine.applyFilter(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true), filterMode)
        val file = File(context.cacheDir, "edited_scan_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(file)
        filtered.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        fos.flush()
        fos.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
