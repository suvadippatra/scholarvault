package com.scholarvault.ui.tools.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.app.Activity
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import com.scholarvault.ui.viewmodel.DocumentViewModel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerCaptureScreen(onBack: () -> Unit, docViewModel: DocumentViewModel, scanId: String? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scannerViewModel = getScannerViewModel(context)
    
    var scannedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val prefs = context.getSharedPreferences("scanner_draft", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        if (scanId == null) {
            val savedDraftStr = prefs.getString("draft_uris", null)
            if (savedDraftStr != null) {
                try {
                    val array = org.json.JSONArray(savedDraftStr)
                    val draftUris = (0 until array.length()).map { Uri.parse(array.getString(it)) }
                    if (draftUris.isNotEmpty()) {
                        scannedUris = draftUris
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
    
    LaunchedEffect(scannedUris) {
        if (scanId == null) {
            val array = org.json.JSONArray()
            scannedUris.forEach { array.put(it.toString()) }
            prefs.edit().putString("draft_uris", array.toString()).apply()
        }
    }
    var loadedSession by remember { mutableStateOf(false) }

    LaunchedEffect(scanId) {
        if (scanId != null && !loadedSession) {
            val sessionFlow = scannerViewModel.allScans.value.find { it.id == scanId }
            if (sessionFlow != null) {
                try {
                    val filePaths = org.json.JSONArray(sessionFlow.pagePaths)
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until filePaths.length()) {
                        val path = filePaths.getString(i)
                        if (!path.endsWith(".pdf", ignoreCase = true)) {
                            uris.add(Uri.fromFile(java.io.File(path)))
                        } else {
                            // It's a PDF export, we can extract just the PDF path, but normally a session should have raw JPGs.
                            uris.add(Uri.fromFile(java.io.File(path)))
                        }
                    }
                    scannedUris = uris
                } catch (e: Exception) {}
            }
            loadedSession = true
        }
    }

    var isProcessing by remember { mutableStateOf(false) }
    val appPrefs = remember { com.scholarvault.data.AppPreferences(context) }
    val scannerEngine by appPrefs.scannerEngine.collectAsState(initial = "custom")
    var hasAutoLaunched by remember { mutableStateOf(false) }
    var showCustomScanner by remember { mutableStateOf(false) }
    var autoLaunchSystemScanner by remember { mutableStateOf(false) }

    LaunchedEffect(scannedUris, scannerEngine) {
        if (scannedUris.isEmpty() && !hasAutoLaunched && scanId == null) {
            hasAutoLaunched = true
            if (scannerEngine == "system") {
                autoLaunchSystemScanner = true
            } else {
                showCustomScanner = true
            }
        }
    }
    
    var currentWorkId by remember { mutableStateOf<java.util.UUID?>(null) }
    var showSuccessSheet by remember { mutableStateOf(false) }
    var exportedFileName by remember { mutableStateOf("") }
    var exportedFilePath by remember { mutableStateOf("") }
    var exportedFileSize by remember { mutableStateOf("") }
    var exportedAuthor by remember { mutableStateOf("") }
    var exportedSubject by remember { mutableStateOf("") }
    
    val workInfoState = remember(currentWorkId) { 
        if (currentWorkId != null) androidx.work.WorkManager.getInstance(context).getWorkInfoByIdFlow(currentWorkId!!) else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(
        initial = null,
        context = kotlin.coroutines.EmptyCoroutineContext
    )

    LaunchedEffect(workInfoState.value) {
        val info = workInfoState.value
        if (info?.state == androidx.work.WorkInfo.State.SUCCEEDED) {
            val data = info.outputData
            exportedFileName = data.getString("fileName") ?: ""
            exportedFilePath = data.getString("filePath") ?: ""
            val f = java.io.File(exportedFilePath)
            if (f.exists()) {
                exportedFileSize = "%.2f MB".format(f.length() / (1024f * 1024f))
            }
            prefs.edit().remove("draft_uris").apply() // clear draft upon exported
            showSuccessSheet = true
            isProcessing = false
        } else if (info?.state == androidx.work.WorkInfo.State.FAILED) {
            Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
            isProcessing = false
        }
    }

    if (showCustomScanner) {
        CustomCameraScannerScreen(
            onBack = { showCustomScanner = false },
            onImagesCaptured = { uris ->
                scannedUris = scannedUris + uris
                showCustomScanner = false
            }
        )
        return
    }
    
    // Scanner Options
    var enableOcr by remember { mutableStateOf(false) }
    var compressionLevel by remember { mutableStateOf(0f) } // 0=Low, 1=Med, 2=High
    
    val totalRawBytes = remember(scannedUris) {
        scannedUris.sumOf { uri -> File(uri.path!!).length() }
    }
    
    val estimatedSizeText by remember(totalRawBytes, compressionLevel) {
        androidx.compose.runtime.derivedStateOf {
            if (totalRawBytes == 0L) ""
            else {
                val rawMb = totalRawBytes / (1024f * 1024f)
                val ratio = when (compressionLevel.toInt()) {
                    0 -> 1.0f    // Low Compression
                    1 -> 0.6f    // Med Compression
                    else -> 0.3f // High Compression
                }
                "Estimated Size: ~%.2f MB".format(rawMb * ratio)
            }
        }
    }
    
    val paperSizes = listOf("A4", "A3", "Legal", "Letter", "Custom (1000x1000)")
    var selectedSize by remember { mutableStateOf("A4") }
    var isSizeDropdownExpanded by remember { mutableStateOf(false) }
    
    val fitModes = listOf("Full Fit", "Full Width", "Full Height", "Stretched", "Zoomed Out")
    var selectedFit by remember { mutableStateOf("Full Fit") }
    var isFitDropdownExpanded by remember { mutableStateOf(false) }

    val filterPresets = listOf("Original", "Magic Enhance", "Grayscale", "B&W Document")
    var selectedFilter by remember { mutableStateOf("Original") }
    var isFilterDropdownExpanded by remember { mutableStateOf(false) }
    
    val dpiModes = listOf("300 DPI", "150 DPI", "72 DPI")
    var selectedDpi by remember { mutableStateOf("300 DPI") }
    var isDpiDropdownExpanded by remember { mutableStateOf(false) }

    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(100)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    val scanner = GmsDocumentScanning.getClient(options)

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.let { pages ->
                scannedUris = pages.map { it.imageUri }
            }
        }
    }

    fun startScan() {
        val activity = context as Activity
        scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to launch scanner", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(autoLaunchSystemScanner) {
        if (autoLaunchSystemScanner) {
            autoLaunchSystemScanner = false
            startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (scannedUris.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No pages scanned yet.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showCustomScanner = true }) {
                            Text("Scan with Custom Camera")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { startScan() }) {
                            Text("Scan with ML Kit API")
                        }
                    }
                }
            } else {
                var editorUri by remember { mutableStateOf<Uri?>(null) }

                if (editorUri != null) {
                    ScannedPageEditorScreen(
                        uri = editorUri!!,
                        initialFilter = "Original",
                        onDismiss = { editorUri = null },
                        onSave = { newUri, _ ->
                            val index = scannedUris.indexOf(editorUri!!)
                            if (index != -1) {
                                val updated = scannedUris.toMutableList()
                                updated[index] = newUri
                                scannedUris = updated
                            }
                            editorUri = null
                        }
                    )
                } else {
                    LazyRow(
                    modifier = Modifier.weight(0.3f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(scannedUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(0.7f)
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Scanned Page",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { editorUri = uri }
                            )
                            
                            // Reordering arrows
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { 
                                            val mList = scannedUris.toMutableList()
                                            java.util.Collections.swap(mList, index, index - 1)
                                            scannedUris = mList
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.ArrowBack, "", tint = Color.White) }
                                }
                                if (index < scannedUris.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val mList = scannedUris.toMutableList()
                                            java.util.Collections.swap(mList, index, index + 1)
                                            scannedUris = mList
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.ArrowForward, "", tint = Color.White) }
                                }
                            }
                            
                            // Delete button and page indicator
                            Text(
                                "Page ${index + 1}", 
                                color = Color.White, 
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            )
                            IconButton(
                                onClick = { 
                                    scannedUris = scannedUris.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, "", tint = Color.Red)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(0.7f).fillMaxWidth()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showCustomScanner = true }, modifier = Modifier.weight(1f)) {
                                Text("Add (Camera)")
                            }
                            OutlinedButton(onClick = { startScan() }, modifier = Modifier.weight(1f)) {
                                Text("Add (ML Kit)")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("PDF Options", style = MaterialTheme.typography.titleMedium)
                        
                        // Toggles
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableOcr, onCheckedChange = { enableOcr = it })
                            Text("Extract Text (OCR)")
                        }
                        Text("Compression Level", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                        Slider(
                            value = compressionLevel,
                            onValueChange = { compressionLevel = it },
                            valueRange = 0f..2f,
                            steps = 1
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Low", style = MaterialTheme.typography.bodySmall)
                            Text("Med", style = MaterialTheme.typography.bodySmall)
                            Text("High", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        if (estimatedSizeText.isNotEmpty()) {
                            Text(
                                text = estimatedSizeText, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.secondary, 
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // DPI Setting
                        ExposedDropdownMenuBox(
                            expanded = isDpiDropdownExpanded,
                            onExpandedChange = { isDpiDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedDpi,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Resolution (DPI)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDpiDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isDpiDropdownExpanded,
                                onDismissRequest = { isDpiDropdownExpanded = false }
                            ) {
                                dpiModes.forEach { sz ->
                                    DropdownMenuItem(
                                        text = { Text(sz) },
                                        onClick = { selectedDpi = sz; isDpiDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Filter Preset
                        ExposedDropdownMenuBox(
                            expanded = isFilterDropdownExpanded,
                            onExpandedChange = { isFilterDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filter Preset") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFilterDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isFilterDropdownExpanded,
                                onDismissRequest = { isFilterDropdownExpanded = false }
                            ) {
                                filterPresets.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = { selectedFilter = f; isFilterDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Paper Size
                        ExposedDropdownMenuBox(
                            expanded = isSizeDropdownExpanded,
                            onExpandedChange = { isSizeDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSize,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Paper Size") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSizeDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isSizeDropdownExpanded,
                                onDismissRequest = { isSizeDropdownExpanded = false }
                            ) {
                                paperSizes.forEach { sz ->
                                    DropdownMenuItem(
                                        text = { Text(sz) },
                                        onClick = { selectedSize = sz; isSizeDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Fit Mode
                        ExposedDropdownMenuBox(
                            expanded = isFitDropdownExpanded,
                            onExpandedChange = { isFitDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedFit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Fit Mode") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFitDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isFitDropdownExpanded,
                                onDismissRequest = { isFitDropdownExpanded = false }
                            ) {
                                fitModes.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = { selectedFit = f; isFitDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                val urisArray = org.json.JSONArray()
                                scannedUris.forEach { urisArray.put(it.toString()) }
                                
                                val data = androidx.work.Data.Builder()
                                    .putString("uris", urisArray.toString())
                                    .putBoolean("enableOcr", enableOcr)
                                    .putFloat("compressionLevel", compressionLevel)
                                    .putString("paperSize", selectedSize)
                                    .putString("fitMode", selectedFit)
                                    .putString("filterMode", selectedFilter)
                                    .putString("dpiMode", selectedDpi)
                                    .apply {
                                        scanId?.let { putString("scanId", it) }
                                    }
                                    .build()
                                    
                                val workRequest = androidx.work.OneTimeWorkRequestBuilder<PdfExportWorker>()
                                    .setInputData(data)
                                    .build()
                                    
                                androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
                                
                                isProcessing = true
                                currentWorkId = workRequest.id
                                Toast.makeText(context, "Exporting PDF...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing && scannedUris.isNotEmpty()
                        ) {
                            if (isProcessing) {
                                val info = workInfoState.value
                                val progress = info?.progress?.getInt("progress", 0) ?: 0
                                val max = info?.progress?.getInt("max", 1) ?: 1
                                
                                if (max > 1 && progress > 0) {
                                    CircularProgressIndicator(
                                        progress = progress.toFloat() / max.toFloat(), 
                                        modifier = Modifier.size(24.dp), 
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exporting... ($progress / $max)")
                                } else {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exporting...")
                                }
                            } else {
                                Text("Generate PDF in Background")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = {
                                val pagePaths = org.json.JSONArray()
                                scannedUris.forEach { uri -> pagePaths.put(uri.path) }
                                
                                val entity = com.scholarvault.data.model.ScannedDocumentEntity(
                                    id = scanId ?: java.util.UUID.randomUUID().toString(),
                                    name = "Scan_Session_${System.currentTimeMillis()}",
                                    pagePaths = pagePaths.toString()
                                )
                                scannerViewModel.insertScan(entity)
                                prefs.edit().remove("draft_uris").apply() // Clear draft on implicit save
                                Toast.makeText(context, "Saved Session", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = scannedUris.isNotEmpty() && !isProcessing
                        ) {
                            Text("Save as Draft Session")
                        }
                    }
                }
                }
            }
        }
    }

    if (showSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSuccessSheet = false 
                onBack() 
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Export Successful",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "File Size: $exportedFileSize",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = exportedFileName,
                    onValueChange = { exportedFileName = it },
                    label = { Text("Rename File") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = exportedAuthor,
                    onValueChange = { exportedAuthor = it },
                    label = { Text("Author (PDF Metadata)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = exportedSubject,
                    onValueChange = { exportedSubject = it },
                    label = { Text("Subject / Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        updatePdfMetadata(exportedFilePath, exportedAuthor, exportedSubject)
                        val file = java.io.File(exportedFilePath)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
                    }.padding(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Share")
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        updatePdfMetadata(exportedFilePath, exportedAuthor, exportedSubject)
                        try {
                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, exportedFileName)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                            }
                            val resolver = context.contentResolver
                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                resolver.openOutputStream(uri)?.use { out ->
                                    java.io.File(exportedFilePath).inputStream().use { input ->
                                        input.copyTo(out)
                                    }
                                }
                                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                            }
                        } catch(e: Exception) {
                            Toast.makeText(context, "Failed to save to device", Toast.LENGTH_SHORT).show()
                        }
                    }.padding(8.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Device")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Device")
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        updatePdfMetadata(exportedFilePath, exportedAuthor, exportedSubject)
                        try {
                            val sourceFile = java.io.File(exportedFilePath)
                            val docFile = com.scholarvault.data.model.DocumentFile(
                                name = exportedFileName,
                                isFolder = false,
                                extension = "pdf",
                                sizeBytes = sourceFile.length(),
                                filePath = sourceFile.name,
                                tags = if (exportedSubject.isNotBlank()) exportedSubject.split(",").map{it.trim()}.filter{it.isNotEmpty()} else emptyList()
                            )
                            docViewModel.insertFile(
                                context = context,
                                file = docFile,
                                uri = android.net.Uri.fromFile(sourceFile)
                            )
                            Toast.makeText(context, "Saved to Vault", Toast.LENGTH_SHORT).show()
                            showSuccessSheet = false
                            onBack()
                        } catch(e: Exception) {
                            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.padding(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = "Save to Vault")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Vault")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

fun updatePdfMetadata(filePath: String, author: String, subject: String) {
    if (author.isBlank() && subject.isBlank()) return
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) return
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file)
        val info = document.documentInformation
        if (author.isNotBlank()) info.author = author
        if (subject.isNotBlank()) info.subject = subject
        document.save(file)
        document.close()
    } catch(e: Exception) {
        e.printStackTrace()
    }
}

suspend fun generatePdf(
    context: Context,
    docViewModel: DocumentViewModel,
    scannerViewModel: com.scholarvault.ui.viewmodel.ScannerViewModel,
    uris: List<Uri>,
    enableOcr: Boolean,
    compressionLevel: Float,
    paperSize: String,
    fitMode: String,
    filterMode: String,
    dpiMode: String
) = withContext(Dispatchers.IO) {
    var pdfWidth = 595
    var pdfHeight = 842
    
    when (paperSize) {
        "A3" -> { pdfWidth = 842; pdfHeight = 1190 }
        "A4" -> { pdfWidth = 595; pdfHeight = 842 }
        "Legal" -> { pdfWidth = 612; pdfHeight = 1008 }
        "Letter" -> { pdfWidth = 612; pdfHeight = 792 }
        "Custom (1000x1000)" -> { pdfWidth = 1000; pdfHeight = 1000 }
    }

    val pdfDocument = PdfDocument()
    val paint = Paint()
    
    val dpiScaleFactor = when (dpiMode) {
        "150 DPI" -> 0.5f
        "72 DPI" -> 0.24f
        else -> 1f // 300 DPI
    }
    
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val extractedTexts = mutableListOf<String>()

    for ((index, uri) in uris.withIndex()) {
        // Find dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val compressionScale = when (compressionLevel.toInt()) {
            0 -> 1.0f
            1 -> 0.6f
            else -> 0.3f
        }
        val scale = compressionScale * dpiScaleFactor
        options.inSampleSize = if (scale < 1f) (1f / scale).toInt() else 1
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Less memory
        
        inputStream = context.contentResolver.openInputStream(uri)
        var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        
        if (bitmap == null) continue

        // Apply native filters if any
        if (filterMode != "Original") {
            val filteredBitmap = FilterEngine.applyFilter(bitmap, filterMode)
            if (filteredBitmap != bitmap) {
                bitmap.recycle()
                bitmap = filteredBitmap
            }
        }

        if (enableOcr) {
            val image = InputImage.fromBitmap(bitmap, 0)
            try {
                val latch = CountDownLatch(1)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        extractedTexts.add("Page ${index + 1}:\n${result.text}\n")
                        latch.countDown()
                    }
                    .addOnFailureListener {
                        latch.countDown()
                    }
                latch.await(30, TimeUnit.SECONDS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Since we already downscaled using inSampleSize and RGB_565, we can use the bitmap directly.
        // It saves us an extra memory allocation here.
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Apply Fit Mode
        val destRect = android.graphics.RectF(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat())
        
        when (fitMode) {
            "Full Fit" -> {
                val scale = minOf(pdfWidth.toFloat() / bitmap.width, pdfHeight.toFloat() / bitmap.height)
                val nw = bitmap.width * scale
                val nh = bitmap.height * scale
                val dx = (pdfWidth - nw) / 2f
                val dy = (pdfHeight - nh) / 2f
                destRect.set(dx, dy, dx + nw, dy + nh)
            }
            "Full Width" -> {
                val scale = pdfWidth.toFloat() / bitmap.width
                val nh = bitmap.height * scale
                destRect.set(0f, 0f, pdfWidth.toFloat(), nh)
            }
            "Full Height" -> {
                val scale = pdfHeight.toFloat() / bitmap.height
                val nw = bitmap.width * scale
                destRect.set(0f, 0f, nw, pdfHeight.toFloat())
            }
            "Stretched" -> {
                destRect.set(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat())
            }
            "Zoomed Out" -> {
                val scale = minOf(pdfWidth.toFloat() / bitmap.width, pdfHeight.toFloat() / bitmap.height) * 0.8f
                val nw = bitmap.width * scale
                val nh = bitmap.height * scale
                val dx = (pdfWidth - nw) / 2f
                val dy = (pdfHeight - nh) / 2f
                destRect.set(dx, dy, dx + nw, dy + nh)
            }
        }

        canvas.drawBitmap(bitmap, null, destRect, paint)
        
        // Add OCR text invisibly or visually if needed
        if (enableOcr) {
            // Drawing text visibly just to show OCR worked, though usually a PDF scanner with OCR creates hidden text layer.
            // But android PdfDocument doesn't support structured text.
        }

        pdfDocument.finishPage(page)
        bitmap.recycle()
    }

    val fileName = "Scanned_Document_${System.currentTimeMillis()}.pdf"
    val file = File(context.filesDir, fileName)
    val fos = FileOutputStream(file)
    pdfDocument.writeTo(fos)
    pdfDocument.close()
    fos.close()

    if (enableOcr && extractedTexts.isNotEmpty()) {
        val txtFile = File(context.filesDir, fileName.replace(".pdf", "_ocr.txt"))
        txtFile.writeText(extractedTexts.joinToString("\n---\n"))
        // OCR text is available alongside
    }

    val pagePaths = org.json.JSONArray()
    pagePaths.put(file.absolutePath)
    
    val entity = com.scholarvault.data.model.ScannedDocumentEntity(
        name = fileName,
        pagePaths = pagePaths.toString()
    )
    scannerViewModel.insertScan(entity)
    
    val finalSizeMb = "%.2f".format(file.length() / (1024f * 1024f))
    withContext(Dispatchers.Main) {
        scannerViewModel.setNewlyGeneratedScan(entity)
        Toast.makeText(context, "Saved $fileName ($finalSizeMb MB)", Toast.LENGTH_SHORT).show()
    }
}

fun FileProviderUri(context: Context, file: File): Uri {
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
