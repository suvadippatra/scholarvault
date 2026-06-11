package com.scholarvault.ui.tools.pdf_inverter

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.tools.AddFilesMenuButton
import com.scholarvault.ui.tools.FileDropBox

import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfColorInverterScreen(
    onBack: () -> Unit,
    viewModel: PdfInverterViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.scholarvault.MainApplication
    val docRepository = remember { com.scholarvault.data.repository.DocumentRepository(app.database.documentDao(), app.database.walletDao()) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val items by viewModel.items.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewMode by remember { mutableStateOf<InvertMode?>(null) }
    var previewPagesToInvert by remember { mutableStateOf("All") }
    var previewOnModeSelected by remember { mutableStateOf<((InvertMode) -> Unit)?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    // Dialog state
    var itemToSaveAppFolder by remember { mutableStateOf<PdfItem?>(null) }
    var itemToSaveDownloads by remember { mutableStateOf<PdfItem?>(null) }
    var showBatchSaveAppFolder by remember { mutableStateOf(false) }
    
    var customFileName by remember { mutableStateOf("") }
    var customTags by remember { mutableStateOf("Inverted") }
    var batchTags by remember { mutableStateOf("Inverted") }

    val processingState by PdfProcessingRepository.processingState.collectAsState()
    
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addUris(
            uris = uris,
            onLimitExceeded = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("You can only add up to 20 PDFs at once.")
                }
            },
            onWarning = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Warning: Adding more than 5 PDFs might take a long time and use more resources.")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (com.scholarvault.ui.tools.SharedData.pendingUris.value.isNotEmpty()) {
            val uris = com.scholarvault.ui.tools.SharedData.pendingUris.value.filter {
                it.toString().endsWith(".pdf", ignoreCase = true) || context.contentResolver.getType(it) == "application/pdf"
            }
            com.scholarvault.ui.tools.SharedData.pendingUris.value = emptyList()
            if (uris.isNotEmpty()) {
                viewModel.addUris(
                    uris = uris,
                    onLimitExceeded = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("You can only add up to 20 PDFs at once.")
                        }
                    },
                    onWarning = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Warning: Adding more than 5 PDFs might take a long time and use more resources.")
                        }
                    }
                )
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 840
    var showDropBox by remember { mutableStateOf(isLargeScreen) }

    if (previewUri != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            PdfPreviewScreen(
                uri = previewUri!!,
                initialMode = previewMode,
                pagesToInvertStr = previewPagesToInvert,
                onModeSelected = previewOnModeSelected,
                onDismiss = { 
                    previewUri = null 
                    previewMode = null
                    previewOnModeSelected = null
                }
            )
        }
        return
    }

    if (showHistory) {
        PdfHistoryScreen(
            onBack = { showHistory = false },
            onPreview = { uri -> 
                previewUri = uri
                previewMode = null
                previewOnModeSelected = null
                showHistory = false 
            }
        )
        return
    }

    // --- Save Dialog: Single To App Folder ---
    if (itemToSaveAppFolder != null) {
        val item = itemToSaveAppFolder!!
        AlertDialog(
            onDismissRequest = { itemToSaveAppFolder = null },
            title = { Text("Save to My Files") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Register this inverted document into the app's secure files explorer.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTags,
                        onValueChange = { customTags = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetName = customFileName
                    val targetTags = customTags
                    itemToSaveAppFolder = null
                    coroutineScope.launch {
                        val processor = PdfInverterProcessor()
                        val uri = item.resultUri ?: processor.processPdfInversion(context, item)
                        if (uri != null) {
                            val success = PdfInverterProcessor.saveItemToAppFolder(context, uri, targetName, targetTags, docRepository)
                            if (success) {
                                snackbarHostState.showSnackbar("Saved to My Files securely!")
                            } else {
                                snackbarHostState.showSnackbar("Save failed.")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Failed to process document.")
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToSaveAppFolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Save Dialog: Single To Downloads ---
    if (itemToSaveDownloads != null) {
        val item = itemToSaveDownloads!!
        AlertDialog(
            onDismissRequest = { itemToSaveDownloads = null },
            title = { Text("Save to Downloads") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Save inverted document to public Downloads folder.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetName = customFileName
                    itemToSaveDownloads = null
                    coroutineScope.launch {
                        val processor = PdfInverterProcessor()
                        val uri = item.resultUri ?: processor.processPdfInversion(context, item)
                        if (uri != null) {
                            val finalUri = PdfInverterProcessor.saveItemToDownloads(context, uri, targetName)
                            if (finalUri != null) {
                                snackbarHostState.showSnackbar("Saved to Downloads!")
                            } else {
                                snackbarHostState.showSnackbar("Save failed.")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Failed to process document.")
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToSaveDownloads = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Save Dialog: Batch To App Folder ---
    if (showBatchSaveAppFolder) {
        AlertDialog(
            onDismissRequest = { showBatchSaveAppFolder = false },
            title = { Text("Batch Save to My Files") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This will process and save all (${items.size}) files into your App Documents folder.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = batchTags,
                        onValueChange = { batchTags = it },
                        label = { Text("Tags for all documents") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetTags = batchTags
                    showBatchSaveAppFolder = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Processing files in background...")
                        var successCount = 0
                        items.forEach { item ->
                            val processor = PdfInverterProcessor()
                            val uri = item.resultUri ?: processor.processPdfInversion(context, item)
                            if (uri != null) {
                                val success = PdfInverterProcessor.saveItemToAppFolder(context, uri, item.newName, targetTags, docRepository)
                                if (success) successCount++
                            }
                        }
                        snackbarHostState.showSnackbar("Successfully saved $successCount file(s) to My Files!")
                    }
                }) {
                    Text("Save All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchSaveAppFolder = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "PDF Color Inverter",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, "Previous Inverted PDFs")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background, contentPadding = PaddingValues(horizontal = 16.dp)) {
                if (!isLargeScreen && !showDropBox) {
                    AddFilesMenuButton(
                        onDeviceClick = { filePicker.launch("application/pdf") },
                        onUrisSelected = { uris -> 
                            viewModel.addUris(
                                uris = uris,
                                onLimitExceeded = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("You can only add up to 20 PDFs at once.")
                                    }
                                },
                                onWarning = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Warning: Adding more than 5 PDFs might take a long time and use more resources.")
                                    }
                                }
                            ) 
                        },
                        onLongPress = { showDropBox = true },
                        label = "Add PDF"
                    )
                }
                Spacer(Modifier.weight(1f))
                
                var expandedSaveMenu by remember { mutableStateOf(false) }

                Box {
                    val isRunning = processingState is PdfProcessingState.Processing
                    val btnContainerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    FloatingActionButton(
                        onClick = {
                            if (isRunning) {
                                // Cancel
                                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("PdfInversionBatch")
                                PdfProcessingRepository.updateState(PdfProcessingState.Idle)
                            } else if (items.isNotEmpty()) {
                                viewModel.processAll()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Background processing started")
                                }
                            }
                        },
                        containerColor = btnContainerColor,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel", color = MaterialTheme.colorScheme.onErrorContainer)
                            } else {
                                Icon(Icons.Default.Check, "Process All")
                                Spacer(Modifier.width(8.dp))
                                Text("Process All")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                if (processingState is PdfProcessingState.Processing) {
                    val pState = processingState as PdfProcessingState.Processing
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Processing: ${pState.fileName}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { pState.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text("${pState.progress}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                if (showDropBox) {
                    FileDropBox(
                        onUrisSelected = { uris ->
                            viewModel.addUris(
                                uris = uris,
                                onLimitExceeded = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("You can only add up to 20 PDFs at once.")
                                    }
                                },
                                onWarning = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Warning: Adding more than 5 PDFs might take a long time and use more resources.")
                                    }
                                }
                            )
                            showDropBox = isLargeScreen
                        },
                        onClose = { if (!isLargeScreen) showDropBox = false },
                        mimeType = "application/pdf"
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text("Imported PDFs (${items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            
            items(items, key = { it.id }) { item ->
                PdfItemCard(
                    item = item,
                    onRemove = { viewModel.removeItem(item.id) },
                    onNameChange = { viewModel.updateItemNewName(item.id, it) },
                    onPagesChange = { viewModel.updateItemPagesToInvert(item.id, it) },
                    onModeChange = { viewModel.updateItemMode(item.id, it) },
                    onPreview = { uri, mode, pagesStr, onModeSel -> 
                        previewUri = uri
                        previewMode = mode
                        previewPagesToInvert = pagesStr
                        previewOnModeSelected = onModeSel
                    },
                    onSaveToApp = {
                        itemToSaveAppFolder = item
                        customFileName = item.newName
                        customTags = "Inverted"
                    },
                    onSaveToDownloads = {
                        itemToSaveDownloads = item
                        customFileName = item.newName
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, item.resultUri!!)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Inverted PDF"))
                    },
                    onPrint = {
                        com.scholarvault.ui.tools.SharedData.pendingUris.value = listOf(item.resultUri!!)
                        com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = true
                    }
                )
            }
        }
    }
}
