package com.scholarvault.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.theme.LocalThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp > 600
    var showDropBox by remember { mutableStateOf(isTablet) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isPdf by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    
    // Original properties
    var originalSizeStr by remember { mutableStateOf("") }
    var originalSizeBytes by remember { mutableStateOf(0L) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Settings
    var compressPercent by remember { mutableStateOf(50f) }
    var pdfMode by remember { mutableStateOf("image") } // "image" or "vector"

    // Compressed properties
    var compressedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var compressedSizeBytes by remember { mutableStateOf(0L) }
    var compressedSizeStr by remember { mutableStateOf("") }
    var isCalculating by remember { mutableStateOf(false) }
    
    var compressedPdfFile by remember { mutableStateOf<File?>(null) }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val handleUris = { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val uri = uris.first()
            selectedUri = uri
            val type = context.contentResolver.getType(uri)
            isPdf = type == "application/pdf"
            fileName = getFileName(context, uri)
            
            if (isPdf) {
                // Determine PDF Size
                scope.launch {
                    val size = getFileSize(context, uri)
                    originalSizeBytes = size
                    originalSizeStr = formatBytes(size)
                    originalBitmap = null
                    compressedBitmap = null
                    compressedPdfFile = null
                    
                    if (pdfMode == "image") {
                        isCalculating = true
                        compressPdfImageBased(context, uri, compressPercent) { tempPdf, cSize ->
                            compressedPdfFile = tempPdf
                            compressedSizeBytes = cSize
                            compressedSizeStr = formatBytes(cSize)
                            isCalculating = false
                        }
                    } else if (pdfMode == "vector") {
                        isCalculating = true
                        compressPdfVectorPreserving(context, uri, compressPercent) { tempPdf, cSize ->
                            compressedPdfFile = tempPdf
                            compressedSizeBytes = cSize
                            compressedSizeStr = formatBytes(cSize)
                            isCalculating = false
                        }
                    }
                }
            } else {
                // Load Image exact
                isCalculating = true
                scope.launch {
                    val bmp = loadOriginalBitmapExact(context, uri)
                    if (bmp != null) {
                        originalBitmap = bmp
                        val size = getFileSize(context, uri)
                        originalSizeBytes = size
                        originalSizeStr = formatBytes(size)
                        // Trigger calculation
                        recalculateCompressionExact(bmp, compressPercent) { cBmp, cSize ->
                            compressedBitmap = cBmp
                            compressedSizeBytes = cSize
                            compressedSizeStr = formatBytes(cSize)
                            isCalculating = false
                        }
                    } else {
                        isCalculating = false
                        Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleUris(listOf(uri))
        }
    }

    var showExportSheet by remember { mutableStateOf(false) }

    // When slider changes, debounce real-time calculation
    LaunchedEffect(compressPercent, pdfMode) {
        if (selectedUri != null) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                if (!isPdf && originalBitmap != null) {
                    isCalculating = true
                    delay(300) // Debounce
                    recalculateCompressionExact(originalBitmap!!, compressPercent) { cBmp, cSize ->
                        compressedBitmap = cBmp
                        compressedSizeBytes = cSize
                        compressedSizeStr = formatBytes(cSize)
                        isCalculating = false
                    }
                } else if (isPdf) {
                    isCalculating = true
                    delay(500) // Longer debounce for PDF
                    if (pdfMode == "image") {
                        compressPdfImageBased(context, selectedUri!!, compressPercent) { tempPdf, cSize ->
                            compressedPdfFile = tempPdf
                            compressedSizeBytes = cSize
                            compressedSizeStr = formatBytes(cSize)
                            isCalculating = false
                        }
                    } else if (pdfMode == "vector") {
                        compressPdfVectorPreserving(context, selectedUri!!, compressPercent) { tempPdf, cSize ->
                            compressedPdfFile = tempPdf
                            compressedSizeBytes = cSize
                            compressedSizeStr = formatBytes(cSize)
                            isCalculating = false
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Compress File",
                showProfileIcon = false,
                showSearchBar = false
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
            if (selectedUri == null) {
                if (showDropBox) {
                    com.scholarvault.ui.tools.FileDropBox(
                        onUrisSelected = handleUris,
                        onClose = if (!isTablet) { { showDropBox = false } } else null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            com.scholarvault.ui.tools.AddFilesMenuButton(
                                onDeviceClick = { filePickerLauncher.launch("*/*") },
                                onUrisSelected = handleUris,
                                onLongPress = { showDropBox = true },
                                label = "Select File"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Supports JPG, PNG, WEBP, PDF", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isPdf) "PDF File" else "Image File", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { selectedUri = null; originalBitmap = null; compressedBitmap = null; compressedPdfFile = null }) {
                                Text("Change")
                            }
                        }
                        Text(fileName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }

                if (isPdf) {
                    // PDF Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { pdfMode = "image" }
                                .background(if (pdfMode == "image") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image-Based", fontWeight = FontWeight.Bold, color = if (pdfMode == "image") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { pdfMode = "vector" }
                                .background(if (pdfMode == "vector") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Vector-Preserve", fontWeight = FontWeight.Bold, color = if (pdfMode == "vector") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Vector or Image based Slider & Tooltip
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (pdfMode == "vector") "Preserves text/shapes. Only compresses embedded images (slower, high fidelity)." 
                                    else "Flattens entire PDF into images. Text gets rasterized (faster, but loss of text searchability/crispness).",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (pdfMode == "vector") "Image Optimization Quality" else "Rasterize Scale Level", fontWeight = FontWeight.Bold)
                                Text("${compressPercent.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = compressPercent,
                                onValueChange = { compressPercent = it },
                                valueRange = 10f..100f
                            )
                            Text(
                                if (pdfMode == "vector") "Lower percentage heavily compresses embedded images." 
                                else "Lower percentage creates smaller dimensions & fuzzier rasterized text.",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Comparison for PDF
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Original", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(originalSizeStr, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Compressed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (isCalculating) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("...", fontSize = 24.sp)
                                        }
                                    } else {
                                        Text(compressedSizeStr, fontSize = 24.sp, color = if (compressedSizeBytes < originalSizeBytes) Color(0xFF43A047) else MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isCalculating && compressedPdfFile != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Compressed PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Image Exact Preview - Side by Side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Original
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                            if (originalBitmap != null) {
                                Image(
                                    bitmap = originalBitmap!!.asImageBitmap(),
                                    contentDescription = "Original",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha=0.6f)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Original", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(originalSizeStr, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                        // Compressed
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                            if (compressedBitmap != null) {
                                Image(
                                    bitmap = compressedBitmap!!.asImageBitmap(),
                                    contentDescription = "Compressed",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (isCalculating) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                            }
                            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha=0.6f)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Compressed", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isCalculating) "..." else compressedSizeStr, color = Color.Green, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    
                    // Slider
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Compression Level", fontWeight = FontWeight.Bold)
                                Text("${compressPercent.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = compressPercent,
                                onValueChange = { compressPercent = it },
                                valueRange = 1f..100f
                            )
                            Text("1% = Max Compression (Lowest Quality) | 100% = Low Compression (Best Quality)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    
                    Button(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isCalculating && compressedBitmap != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Compressed Image", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showExportSheet) {
        if (isPdf && compressedPdfFile != null) {
            val resultUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                compressedPdfFile!!
            )
            SaveDestinationBottomSheet(
                fileUri = resultUri,
                defaultFileName = "compressed_$fileName",
                onDismiss = { showExportSheet = false },
                onSuccess = { fullName ->
                    Toast.makeText(context, "$fullName saved successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        } else if (!isPdf && compressedBitmap != null) {
            // Save the exact preview compressed bytes into a temp file for Export
            var tempFileToExport by remember { mutableStateOf<File?>(null) }
            
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val temp = File(context.cacheDir, "comp_${System.currentTimeMillis()}.jpg")
                    val outStr = ByteArrayOutputStream()
                    originalBitmap!!.compress(Bitmap.CompressFormat.JPEG, compressPercent.toInt().coerceAtLeast(1), outStr)
                    FileOutputStream(temp).use { it.write(outStr.toByteArray()) }
                    tempFileToExport = temp
                }
            }

            if (tempFileToExport != null) {
                val resultUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFileToExport!!
                )
                SaveDestinationBottomSheet(
                    fileUri = resultUri,
                    defaultFileName = "compressed_$fileName".replace(".png", ".jpg").replace(".webp", ".jpg"),
                    onDismiss = { showExportSheet = false },
                    onSuccess = { fullName ->
                        Toast.makeText(context, "$fullName saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------- //
// Helper native strict execution functions
// ----------------------------------------------------- //

private suspend fun compressPdfVectorPreserving(
    context: Context,
    uri: Uri,
    percent: Float, // 10 to 100
    onResult: (File, Long) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext
        val inputStream = java.io.FileInputStream(parcelFileDescriptor.fileDescriptor)
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
        val quality = percent / 100f

        for (page in document.pages) {
            val resources = page.resources ?: continue
            for (name in resources.xObjectNames.toList()) {
                if (resources.isImageXObject(name)) {
                    val pdImage = resources.getXObject(name) as? com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject ?: continue
                    if (pdImage.width > 200 && pdImage.height > 200) {
                        val originalBitmap = pdImage.image
                        if (originalBitmap != null) {
                            val compressedImage = com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(
                                document, originalBitmap, quality
                            )
                            resources.put(name, compressedImage)
                            originalBitmap.recycle()
                        }
                    }
                }
            }
        }
        
        val tempFile = File(context.cacheDir, "comp_pdf_vec_${System.currentTimeMillis()}.pdf")
        document.save(tempFile)
        document.close()
        inputStream.close()
        parcelFileDescriptor.close()
        
        val newSize = tempFile.length()
        withContext(Dispatchers.Main) {
            onResult(tempFile, newSize)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun compressPdfImageBased(
    context: Context,
    uri: Uri,
    percent: Float, // 10 to 100
    onResult: (File, Long) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext
        val renderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
        
        val newPdf = android.graphics.pdf.PdfDocument()
        
        // Scale down dimensions for smaller size.
        val scale = (percent / 100f).coerceIn(0.1f, 1f)
        
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            
            val w = (page.width * scale).toInt().coerceAtLeast(1)
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()
            
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, i).create()
            val newPage = newPdf.startPage(pageInfo)
            newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
            newPdf.finishPage(newPage)
            bitmap.recycle()
        }
        
        renderer.close()
        parcelFileDescriptor.close()
        
        val tempFile = File(context.cacheDir, "comp_pdf_${System.currentTimeMillis()}.pdf")
        FileOutputStream(tempFile).use { out ->
            newPdf.writeTo(out)
        }
        newPdf.close()
        
        val newSize = tempFile.length()
        withContext(Dispatchers.Main) {
            onResult(tempFile, newSize)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun loadOriginalBitmapExact(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun recalculateCompressionExact(
    original: Bitmap,
    percent: Float,
    onResult: (Bitmap, Long) -> Unit
) = withContext(Dispatchers.Default) {
    // Treat percent as JPEG quality (0-100)
    // Even at 100%, re-compressing JPEG drops file size. If percent is too low, we can optionally shrink dimensions native.
    // For now we just use compress quality natively.
    val quality = percent.toInt().coerceIn(1, 100)
    val outStream = ByteArrayOutputStream()
    original.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
    val bytesArray = outStream.toByteArray()
    val sizeStr = bytesArray.size.toLong()
    val newBitmap = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.size)
    
    withContext(Dispatchers.Main) {
        onResult(newBitmap, sizeStr)
    }
}

private fun getFileSize(context: Context, uri: Uri): Long {
    var size = 0L
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { size = it.length }
    if (size <= 0) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    }
    return size
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }
    return result ?: "Unknown_File"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}
