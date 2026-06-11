package com.scholarvault.ui.tools.image_resizer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.tools.ImageComparisonSlider
import com.scholarvault.ui.tools.SharedData
import com.scholarvault.ui.tools.FileDropBox
import com.scholarvault.ui.tools.SaveDestinationBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedImageResizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentItem by remember { mutableStateOf<AdvancedImageItem?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    var isFullscreenPreview by remember { mutableStateOf(false) }

    var showExportSheet by remember { mutableStateOf(false) }

    suspend fun createItemFromUri(uri: Uri): AdvancedImageItem? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var name = uri.lastPathSegment ?: "image_${System.currentTimeMillis()}"
            if (name.contains("/")) name = name.substringAfterLast("/")
            val baseName = name.substringBeforeLast(".")
            
            var sizeKb: Long = 0
            if (uri.scheme == "file") {
                sizeKb = File(uri.path ?: "").length() / 1024
            } else {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                sizeKb = (pfd?.statSize ?: 0) / 1024
                pfd?.close()
            }
            
            var inSampleSize = 1
            while ((options.outWidth / inSampleSize) > 4000 || (options.outHeight / inSampleSize) > 4000) {
                inSampleSize *= 2
            }
            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false
            
            var bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            
            var orientation = android.media.ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    orientation = android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (e: Exception) {}
            
            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (bmp != null) {
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            }
            
            if (bmp != null) {
                AdvancedImageItem(uri, baseName, bmp, null, null, customName = baseName, origSizeKB = sizeKb, targetWidth = bmp.width.toString(), targetHeight = bmp.height.toString())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun handleSelectedUri(uri: Uri) {
        coroutineScope.launch {
            isProcessing = true
            currentItem = createItemFromUri(uri)
            isProcessing = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                currentItem = createItemFromUri(uri)
                isProcessing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (SharedData.pendingUris.value.isNotEmpty()) {
            val uri = SharedData.pendingUris.value.first()
            SharedData.pendingUris.value = emptyList()
            coroutineScope.launch {
                isProcessing = true
                currentItem = createItemFromUri(uri)
                isProcessing = false
            }
        }
    }
    
    suspend fun doProcessImage() {
        val item = currentItem ?: return
        isProcessing = true
        withContext(Dispatchers.Default) {
            val targetW = item.targetWidth.toIntOrNull() ?: 0
            val targetH = item.targetHeight.toIntOrNull() ?: 0
            val targetDpi = item.targetDpi.toIntOrNull() ?: 300
            val targetKb = item.targetKB.toIntOrNull()
            val format = item.format
            val outName = "${item.customName}_final.${if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"}"

            val inputBmp = item.croppedBitmap ?: item.origBitmap
            
            var processedBmp = inputBmp
            val tempDisplayBmp = if (item.removeBackground) FloodFillBackgroundRemoval.removeBackground(inputBmp, item.bgTolerance, item.bgColor) else inputBmp
            
            val finalFile = FloodFillBackgroundRemoval.processImageFinal(
                context, inputBmp, item.removeBackground, item.bgTolerance, item.bgColor,
                targetW, targetH, targetDpi, targetKb, format, outName
            )
            
            if (finalFile != null) {
                val finalUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
                val newSize = finalFile.length() / 1024
                
                withContext(Dispatchers.Main) {
                    currentItem = currentItem?.copy(
                        processedBitmap = tempDisplayBmp,
                        resultUri = finalUri,
                        resultSizeKB = newSize
                    )
                }
            }
        }
        isProcessing = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Pro Image Editor",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    if (currentItem != null) {
                        IconButton(onClick = { currentItem = null }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help")
                    }
                }
            )
        }
    ) { padding ->
        if (currentItem == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                FileDropBox(
                    onUrisSelected = { uris ->
                        uris.firstOrNull()?.let { handleSelectedUri(it) }
                    },
                    mimeType = "image/*"
                )
            }
        } else {
            val item = currentItem!!
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    
                    // Prefix buttons: always visible when editing
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { showCropDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Crop, "Crop", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Crop")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { isFullscreenPreview = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Default.Fullscreen, "Fullscreen", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Expand")
                        }
                    }
                    
                    // PREVIEW (Comparable)
                    Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(12.dp)) {
                        Box(Modifier.fillMaxSize()) {
                            val activeLeftImage = item.croppedBitmap ?: item.origBitmap
                            val activeRightImage = item.processedBitmap ?: activeLeftImage
                            
                            // To use ImageComparisonSlider we need URIs or we can make a Bitmap slider.
                            // We don't have URIs for intermediate bitmaps easily without caching.
                            // So let's just make a simple side-by-side or split bitmap comparison
                            BitmapComparisonSlider(leftBitmap = activeLeftImage, rightBitmap = activeRightImage)
                            
                            // Stats Overlay
                            Column(modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha=0.6f)).padding(4.dp)) {
                                Text("Original: ${activeLeftImage.width}x${activeLeftImage.height} (${item.origSizeKB}KB)", color = Color.White, fontSize = 10.sp)
                                if (item.resultUri != null) {
                                    val tw = item.targetWidth.ifEmpty { "0" }
                                    val th = item.targetHeight.ifEmpty { "0" }
                                    Text("Final: ${tw}x${th} (${item.resultSizeKB}KB), ${item.targetDpi} DPI", color = Color.Green, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Settings Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = item.customName,
                                onValueChange = { currentItem = item.copy(customName = it) },
                                label = { Text("Rename File") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            // Background Settings
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.removeBackground, onCheckedChange = { currentItem = item.copy(removeBackground = it) })
                                Text("Remove & Replace Background", fontWeight = FontWeight.Bold)
                            }
                            if (item.removeBackground) {
                                Text("Tolerance Slider (0 to 100):", fontSize = 12.sp)
                                Slider(
                                    value = item.bgTolerance,
                                    onValueChange = { currentItem = item.copy(bgTolerance = it) },
                                    valueRange = 0f..100f
                                )
                                Text("Background Color:", fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    CustomBgColor.values().forEach { bgConf ->
                                        val isSel = item.bgColor == bgConf.colorValue
                                        Box(
                                            modifier = Modifier.size(32.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (bgConf == CustomBgColor.TRANSPARENT) Color.Gray else Color(bgConf.colorValue))
                                                .clickable { 
                                                    currentItem = item.copy(
                                                        bgColor = bgConf.colorValue,
                                                        format = if (bgConf == CustomBgColor.TRANSPARENT) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) Icon(Icons.Default.Check, null, tint = if (bgConf == CustomBgColor.WHITE) Color.Black else Color.White)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            Text("Dimensions & Quality", fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = item.targetWidth,
                                    onValueChange = { 
                                        var nw = item.targetHeight
                                        if (item.lockAspect && it.toFloatOrNull() != null) {
                                            val w = it.toFloat()
                                            val r = (item.croppedBitmap ?: item.origBitmap).height.toFloat() / (item.croppedBitmap ?: item.origBitmap).width.toFloat()
                                            nw = (w * r).toInt().toString()
                                        }
                                        currentItem = item.copy(targetWidth = it, targetHeight = nw)
                                    },
                                    label = { Text("Width (px)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = item.targetHeight,
                                    onValueChange = { 
                                        var nw = item.targetWidth
                                        if (item.lockAspect && it.toFloatOrNull() != null) {
                                            val h = it.toFloat()
                                            val r = (item.croppedBitmap ?: item.origBitmap).width.toFloat() / (item.croppedBitmap ?: item.origBitmap).height.toFloat()
                                            nw = (h * r).toInt().toString()
                                        }
                                        currentItem = item.copy(targetHeight = it, targetWidth = nw)
                                    },
                                    label = { Text("Height (px)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.lockAspect, onCheckedChange = { currentItem = item.copy(lockAspect = it) })
                                Text("Lock Aspect Ratio", fontSize = 12.sp)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = item.targetDpi,
                                    onValueChange = { currentItem = item.copy(targetDpi = it) },
                                    label = { Text("DPI") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = item.targetKB,
                                    onValueChange = { currentItem = item.copy(targetKB = it) },
                                    label = { Text("Max Size (KB)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { coroutineScope.launch { doProcessImage() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Process Image")
                    }
                    
                    if (item.resultUri != null) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showExportSheet = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export / Save Resized Image")
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
            
            if (showCropDialog) {
                AdcancedCropDialog(
                    bitmap = item.origBitmap,
                    onDismiss = { showCropDialog = false },
                    onCropped = { newBmp ->
                        currentItem = item.copy(
                            croppedBitmap = newBmp,
                            targetWidth = newBmp.width.toString(),
                            targetHeight = newBmp.height.toString()
                        )
                        showCropDialog = false
                    }
                )
            }
            
            if (isFullscreenPreview) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { isFullscreenPreview = false }) {
                    BitmapComparisonSlider(leftBitmap = item.croppedBitmap ?: item.origBitmap, rightBitmap = item.processedBitmap ?: (item.croppedBitmap ?: item.origBitmap))
                    Text("Tap anywhere to close", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(32.dp))
                }
            }
        }
        
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Editing Guide") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("1. Crop: Frame the subject (face mostly) ensuring an even background, avoid stray hairs.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("2. Background Removal: Tweak the tolerance slider to eliminate background. Pick White, Blue, or Transparent.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("3. Dimensions: Set exact pixels and DPI targets.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("4. Limits: Optionally enforce a maximum KB size.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("5. Export: Tap Process, then Save or Share to your desired destination.", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it") } }
            )
        }
        
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("History") },
                text = {
                    val cacheDir = File(context.cacheDir.absolutePath)
                    val files = cacheDir.listFiles()?.filter { it.name.contains("_final") }?.sortedByDescending { it.lastModified() } ?: emptyList()
                    if (files.isEmpty()) {
                        Text("No recent edits.")
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(files.size) { i ->
                                val f = files[i]
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(f.name, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share"))
                                    }) { Icon(Icons.Default.Share, "Share", modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Close") } }
            )
        }

        if (showExportSheet && currentItem?.resultUri != null) {
            SaveDestinationBottomSheet(
                fileUri = currentItem!!.resultUri!!,
                defaultFileName = "${currentItem!!.customName}_final.${if (currentItem!!.format == Bitmap.CompressFormat.PNG) "png" else "jpg"}",
                onDismiss = { showExportSheet = false },
                onSuccess = { fullName ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Saved successfully: $fullName")
                    }
                }
            )
        }
    }
}
