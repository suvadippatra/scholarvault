package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressImageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalThemeController.current.isDarkTheme

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalSizeStr by remember { mutableStateOf("") }
    var originalWidth by remember { mutableStateOf(0) }
    var originalHeight by remember { mutableStateOf(0) }

    var quality by remember { mutableStateOf(80f) }
    var scalePercent by remember { mutableStateOf(100f) }

    var isCompressing by remember { mutableStateOf(false) }
    var compressedFile by remember { mutableStateOf<File?>(null) }
    var compressedSizeStr by remember { mutableStateOf("") }
    var compressedWidth by remember { mutableStateOf(0) }
    var compressedHeight by remember { mutableStateOf(0) }
    var compressionRatio by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            compressedFile = null // Reset previous compression
            scope.launch {
                getOriginalImageDetails(context, uri) { sizeStr, w, h ->
                    originalSizeStr = sizeStr
                    originalWidth = w
                    originalHeight = h
                }
            }
        }
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    fun handleSelectedUri(uri: Uri) {
        selectedImageUri = uri
        compressedFile = null // Reset previous compression
        scope.launch {
            getOriginalImageDetails(context, uri) { sizeStr, w, h ->
                originalSizeStr = sizeStr
                originalWidth = w
                originalHeight = h
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Compression Guide") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("1. Select an Image: Pick any PNG, JPEG or WEBP photo from your Device, My Files or secure Wallet.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("2. Set Quality: Lower quality values mean smaller files. 70-80% is usually perfect for clear documents with massive size savings.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("3. Set Scaling: If your image size in pixels is very large, slide to scale down (e.g. to 50%) to shrink Kb sizes further.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("4. Compress: Click 'Compress Image Now' to run the fast background reducer.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("5. Export & Save: Use the standard Save panel to share, print, download, or lock the output in your Secure Vault.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it") } }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Recent Compressions") },
            text = {
                val cacheDir = File(context.cacheDir.absolutePath)
                val files = cacheDir.listFiles()?.filter { it.name.startsWith("compressed_") && it.name.endsWith(".jpg") }?.sortedByDescending { it.lastModified() } ?: emptyList()
                if (files.isEmpty()) {
                    Text("No recent compressions found in cache.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(files.size) { i ->
                            val f = files[i]
                            val displaySize = formatBytes(f.length())
                            val dateStr = java.text.SimpleDateFormat("HH:mm, MMM dd", java.util.Locale.getDefault()).format(java.util.Date(f.lastModified()))
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.name.substringAfter("compressed_"), fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("$displaySize • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                                }) { Icon(Icons.Default.Share, "Share", modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Compress Image",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help Information")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Description
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "Image Reducer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Optimize, resize and reduce image file size for student/exam forms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Image Picker / Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri == null) {
                    FileDropBox(
                        onUrisSelected = { uris ->
                            uris.firstOrNull()?.let { handleSelectedUri(it) }
                        },
                        mimeType = "image/*"
                    )
                } else {
                    val displayImage = compressedFile ?: File(getRealPathFromUri(context, selectedImageUri!!) ?: "")
                    if (displayImage.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = displayImage),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(model = selectedImageUri),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Change Image Overlay Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedImageUri = null } // Allow selecting another file by resetting
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Change",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (selectedImageUri != null) {
                // Dimensions & Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Image Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Original Size:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$originalSizeStr ($originalWidth x $originalHeight px)", fontWeight = FontWeight.Medium)
                        }
                        if (compressedFile != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Compressed Size:", color = MaterialTheme.colorScheme.secondary)
                                Text("$compressedSizeStr ($compressedWidth x $compressedHeight px)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reduction Ratio:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(compressionRatio, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Compression Parameters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        // Quality Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Compression Quality", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${quality.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = quality,
                                onValueChange = { quality = it },
                                valueRange = 10f..100f,
                                steps = 17
                            )
                        }

                        // Resize Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dimensions Scaling", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${scalePercent.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = scalePercent,
                                onValueChange = { scalePercent = it },
                                valueRange = 20f..100f,
                                steps = 15
                            )
                        }

                        Button(
                            onClick = {
                                isCompressing = true
                                scope.launch {
                                    runImageCompression(
                                        context = context,
                                        uri = selectedImageUri!!,
                                        quality = quality.toInt(),
                                        scale = scalePercent / 100f
                                    ) { file, sizeStr, w, h, ratio ->
                                        compressedFile = file
                                        compressedSizeStr = sizeStr
                                        compressedWidth = w
                                        compressedHeight = h
                                        compressionRatio = ratio
                                        isCompressing = false
                                        Toast.makeText(context, "Image Compressed successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCompressing
                        ) {
                            if (isCompressing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Text("Compressing...")
                            } else {
                                Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compress Image Now")
                            }
                        }
                    }
                }

                // Sharing and Export Controls
                if (compressedFile != null) {
                    Button(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export / Save Compressed Image")
                    }
                }
            } else {
                // Empty state guidance
                Text(
                    "Please choose an image to configure dynamic compression size.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showExportSheet && compressedFile != null) {
        val resultUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            compressedFile!!
        )
        val defaultName = compressedFile!!.name
        SaveDestinationBottomSheet(
            fileUri = resultUri,
            defaultFileName = defaultName,
            onDismiss = { showExportSheet = false },
            onSuccess = { fullName ->
                Toast.makeText(context, "$fullName saved successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private suspend fun getOriginalImageDetails(
    context: Context,
    uri: Uri,
    onResult: (String, Int, Int) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        var size = 0L
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            size = it.length
        }
        if (size <= 0) {
            val file = File(getRealPathFromUri(context, uri) ?: "")
            if (file.exists()) {
                size = file.length()
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            val sizeStr = formatBytes(size)
            onResult(sizeStr, options.outWidth, options.outHeight)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun runImageCompression(
    context: Context,
    uri: Uri,
    quality: Int,
    scale: Float,
    onResult: (File, String, Int, Int, String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val originalBitmap = BitmapFactory.decodeStream(input)
            if (originalBitmap != null) {
                val newWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
                val newHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val compressedData = outputStream.toByteArray()

                val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out ->
                    out.write(compressedData)
                    out.flush()
                }

                // Original Size determination
                var origSize = 1L
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { origSize = it.length }

                val reduction = if (origSize > 0) {
                    val pct = ((origSize - compressedData.size).toFloat() / origSize.toFloat() * 100f).toInt()
                    "Reduced by $pct%"
                } else "Success"

                onResult(
                    tempFile,
                    formatBytes(compressedData.size.toLong()),
                    newWidth,
                    newHeight,
                    reduction
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun saveToGallery(context: Context, file: File) {
    try {
        val root = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        val destDir = File(root, "ScholarVault")
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, "Compressed_${System.currentTimeMillis()}.jpg")
        file.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Notify MediaScanner
        val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(destFile)
        context.sendBroadcast(mediaScanIntent)

        Toast.makeText(context, "Saved to Pictures/ScholarVault", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving to Pictures category", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Compressed Image"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

private fun getRealPathFromUri(context: Context, contentUri: Uri): String? {
    if (contentUri.scheme == "file") return contentUri.path
    var cursor: android.database.Cursor? = null
    try {
        val proj = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        cursor = context.contentResolver.query(contentUri, proj, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
            return cursor.getString(columnIndex)
        }
    } catch (e: Exception) {
        // Fallback
    } finally {
        cursor?.close()
    }
    return null
}
