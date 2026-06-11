package com.scholarvault.ui.tools.pdf_nup

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scholarvault.ui.components.TopSearchBar
import com.scholarvault.ui.tools.AddFilesMenuButton
import com.scholarvault.ui.tools.FileDropBox
import com.scholarvault.ui.tools.SaveDestinationBottomSheet

import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NUpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: NUpViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    
    var showHelpSheet by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val isLandscapeTablet = configuration.screenWidthDp >= 840 && 
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = isLandscapeTablet
    
    var showDropBox by remember { mutableStateOf(isTablet) }
    var showExactPreview by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.items.isEmpty()) {
        if (viewModel.items.isEmpty()) {
            showExactPreview = false
        }
    }

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach {
                viewModel.addFile(context, it)
            }
        }
    }

    val keyboardController = remember { NUpKeyboardController() }

    var pdfToSaveUri by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    var showRenameDialog by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var renameFileName by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("NUp_Document") }

    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->
        val sourceUri = pdfToSaveUri
        if (destinationUri != null && sourceUri != null) {
            try {
                context.contentResolver.openInputStream(sourceUri).use { input ->
                    context.contentResolver.openOutputStream(destinationUri).use { output ->
                        if (input != null && output != null) {
                            input.copyTo(output)
                            android.widget.Toast.makeText(context, "Saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to write file contents", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error saving: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val onInitiateSave = { uri: android.net.Uri, defaultName: String ->
        pdfToSaveUri = uri
        val cleanDefault = defaultName.replace(".pdf", "", ignoreCase = true)
        renameFileName = cleanDefault
        showRenameDialog = true
    }

    val printPdf = { uri: android.net.Uri, docName: String ->
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        printManager?.let {
            val jobName = docName
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }
                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback?.onWriteFailed(e.localizedMessage)
                    }
                }
            }
            it.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }

    CompositionLocalProvider(LocalNUpKeyboard provides keyboardController) {
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        BackHandler(enabled = keyboardController.isVisible.value) {
            keyboardController.hide()
            focusManager.clearFocus()
        }
        Scaffold(
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                    if (keyboardController.isVisible.value) {
                        val key = keyEvent.utf16CodePoint.toChar()
                        if (key.isDigit() || key == '-' || key == ',' || key == '.') {
                            keyboardController.handleKeyPress(key.toString())
                            return@onPreviewKeyEvent true
                        }
                        if (keyEvent.key == androidx.compose.ui.input.key.Key.Backspace) {
                            keyboardController.handleKeyPress("DEL")
                            return@onPreviewKeyEvent true
                        }
                        if (keyEvent.key == androidx.compose.ui.input.key.Key.Spacebar) {
                            keyboardController.handleKeyPress("SPACE")
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            },
            topBar = {
                TopSearchBar(
                    title = "PDF N-Up",
                    onOpenDrawer = onNavigateBack,
                    isBackButton = true,
                    showProfileIcon = false,
                    showSearchBar = false,
                    actions = {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { showHelpSheet = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                        }
                        
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu, 
                                onDismissRequest = { showMenu = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Merge all files") },
                                    leadingIcon = {
                                        if (config.processingMode == ProcessingMode.MERGE_ALL) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = { 
                                        viewModel.updateConfig { it.copy(processingMode = ProcessingMode.MERGE_ALL) }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Process in parallel") },
                                    leadingIcon = {
                                        if (config.processingMode == ProcessingMode.PARALLEL_BATCH) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = { 
                                        viewModel.updateConfig { it.copy(processingMode = ProcessingMode.PARALLEL_BATCH) }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Photo Grid Repeat") },
                                    leadingIcon = {
                                        if (config.processingMode == ProcessingMode.GRID_REPEAT) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = { 
                                        viewModel.updateConfig { it.copy(processingMode = ProcessingMode.GRID_REPEAT) }
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!keyboardController.isVisible.value) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val viewItems = viewModel.items
                        val isMergedOrGridRepeat = config.processingMode == ProcessingMode.MERGE_ALL || config.processingMode == ProcessingMode.GRID_REPEAT
                        val allSuccess = viewItems.isNotEmpty() && viewItems.all { it.state == NUpProcessingState.SUCCESS }
                        val mergedUri = if (isMergedOrGridRepeat && allSuccess) viewItems.firstOrNull()?.resultUri else null

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side: Import button
                            AddFilesMenuButton(
                                onDeviceClick = { filePicker.launch(arrayOf("application/pdf", "image/*")) },
                                onUrisSelected = { uris ->
                                    uris.forEach {
                                        viewModel.addFile(context, it)
                                    }
                                },
                                onLongPress = { showDropBox = true }
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right side: Process / Save / Share actions
                            if (viewItems.isNotEmpty()) {
                                if (isProcessing) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        val progressVal by viewModel.processProgress.collectAsState()
                                        Text(
                                            text = "${(progressVal * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Button(
                                            onClick = { viewModel.cancelProcessing(context) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    val hasPendingItems = viewItems.any { it.state == NUpProcessingState.PENDING }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (hasPendingItems) {
                                            Text(
                                                text = "Apply changes to update output",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        if (mergedUri != null) {
                                            // Symmetric secondary utility row/group
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                            setDataAndType(mergedUri, "application/pdf")
                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "application/pdf"
                                                            putExtra(android.content.Intent.EXTRA_STREAM, mergedUri)
                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(
                                                    onClick = { printPdf(mergedUri, config.outputFileName) }
                                                ) {
                                                    Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(
                                                    onClick = { onInitiateSave(mergedUri, config.outputFileName + ".pdf") }
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = "Save As", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }

                                        // Consistently show primary action button "Apply Changes" with a check-icon
                                        Button(
                                            onClick = { viewModel.processAll(context) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Apply Changes", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Apply Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = maxOf(0.dp, paddingValues.calculateTopPadding() - 16.dp),
                        end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                val progress by viewModel.processProgress.collectAsState()
                val statusMsg by viewModel.processStatusMessage.collectAsState()

                if (isTablet) {
                    // Dual Column arrangement for larger tablet/foldable screens
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column (Added PDF Documents & Upload list)
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            if (isProcessing) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )
                                Text(
                                    text = statusMsg, 
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                                )
                            }

                            if (showDropBox) {
                                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                    FileDropBox(
                                        onUrisSelected = { uris ->
                                            uris.forEach { viewModel.addFile(context, it) }
                                        },
                                        onClose = { showDropBox = false },
                                        mimeType = "*/*"
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Documents",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { isGridView = !isGridView }) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                                    )
                                }
                            }

                            val tabletColumns = if (isGridView) GridCells.Fixed(3) else GridCells.Fixed(2)
                            LazyVerticalGrid(
                                columns = tabletColumns,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (viewModel.items.isEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No documents loaded. Press the bottom icon to add.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(viewModel.items, key = { it.id }) { item ->
                                        val itemIndex = viewModel.items.indexOf(item)
                                        val isMoveUpEnabled = itemIndex > 0
                                        val isMoveDownEnabled = itemIndex != -1 && itemIndex < viewModel.items.size - 1
                                        NUpFileInputCard(
                                            item = item,
                                            isMoveUpEnabled = isMoveUpEnabled,
                                            isMoveDownEnabled = isMoveDownEnabled,
                                            onMoveUp = { viewModel.moveItemUp(item) },
                                            onMoveDown = { viewModel.moveItemDown(item) },
                                            onPageRangeChange = { newTxt ->
                                                val idx = viewModel.items.indexOf(item)
                                                if (idx != -1) {
                                                    viewModel.items[idx] = item.copy(pageSelectionText = newTxt)
                                                    viewModel.resetSuccessStatesToPending()
                                                }
                                            },
                                            onInversionToggle = {
                                                val idx = viewModel.items.indexOf(item)
                                                if (idx != -1) {
                                                    viewModel.items[idx] = item.copy(isInvertedSelection = !item.isInvertedSelection)
                                                    viewModel.resetSuccessStatesToPending()
                                                }
                                            },
                                            onDpiChange = { newDpi ->
                                                val idx = viewModel.items.indexOf(item)
                                                if (idx != -1) {
                                                    viewModel.items[idx] = item.copy(imageDpiSetting = newDpi)
                                                    viewModel.resetSuccessStatesToPending()
                                                }
                                            },
                                            onRemove = {
                                                viewModel.removePdf(item)
                                            },
                                            onSaveAsClick = { uri ->
                                                onInitiateSave(uri, item.name)
                                            },
                                            onPrintClick = { uri ->
                                                printPdf(uri, item.name)
                                            },
                                            isGridView = isGridView
                                        )
                                    }
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Right Column (Config & Live Rendering Graphical Preview)
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {


                                item {
                                    NUpConfigPanel(
                                        config = config,
                                        onConfigChange = { viewModel.updateConfig { _ -> it } },
                                        onPreviewClick = { showExactPreview = true },
                                        pageNumbersContent = null,
                                        isMobile = false,
                                        itemsEmpty = viewModel.items.isEmpty(),
                                        previewContent = {
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                color = androidx.compose.ui.graphics.Color.Transparent,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                NUpGraphicalPreviewPanel(
                                                    totalOutputPages = viewModel.getExpectedPageCount(config),
                                                    virtualPageSeq = viewModel.virtualPageSequenceGenerator.generateSequence(),
                                                    config = config,
                                                    modifier = Modifier.fillMaxSize().padding(12.dp)
                                                )
                                            }
                                        }
                                    )
                                }

                                item {
                                    androidx.compose.animation.AnimatedVisibility(visible = showExactPreview && viewModel.items.isNotEmpty()) {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth().height(400.dp).padding(vertical = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text("Output Preview", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, top = 12.dp))
                                                NUpPreviewPanel(
                                                    totalOutputPages = viewModel.getExpectedPageCount(config),
                                                    virtualPageSeq = viewModel.virtualPageSequenceGenerator.generateSequence(),
                                                    config = config,
                                                    cacheManager = viewModel.previewCacheManager,
                                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Compact Single Column Layout for Mobile Screens
                    val mobileColumns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1)
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isProcessing) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Text(
                                text = statusMsg, 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                            )
                        }

                        LazyVerticalGrid(
                            columns = mobileColumns,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showDropBox) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    FileDropBox(
                                        onUrisSelected = { uris ->
                                            showDropBox = false
                                            uris.forEach { viewModel.addFile(context, it) }
                                        },
                                        onClose = { showDropBox = false },
                                        mimeType = "*/*"
                                    )
                                }
                            }



                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                NUpConfigPanel(
                                    config = config,
                                    onConfigChange = { viewModel.updateConfig { _ -> it } },
                                    onPreviewClick = { showExactPreview = true },
                                    pageNumbersContent = null,
                                    isMobile = true,
                                    itemsEmpty = viewModel.items.isEmpty(),
                                    previewContent = {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            color = androidx.compose.ui.graphics.Color.Transparent,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            NUpGraphicalPreviewPanel(
                                                totalOutputPages = viewModel.getExpectedPageCount(config),
                                                virtualPageSeq = viewModel.virtualPageSequenceGenerator.generateSequence(),
                                                config = config,
                                                modifier = Modifier.fillMaxSize().padding(12.dp)
                                            )
                                        }
                                    }
                                )
                            }
                            
                            if (viewModel.items.isNotEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Documents",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { isGridView = !isGridView }) {
                                            Icon(
                                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                                contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                                            )
                                        }
                                    }
                                }
                            }
                            
                            items(viewModel.items, key = { it.id }) { item ->
                                val itemIndex = viewModel.items.indexOf(item)
                                val isMoveUpEnabled = itemIndex > 0
                                val isMoveDownEnabled = itemIndex != -1 && itemIndex < viewModel.items.size - 1
                                NUpFileInputCard(
                                    item = item,
                                    isMoveUpEnabled = isMoveUpEnabled,
                                    isMoveDownEnabled = isMoveDownEnabled,
                                    onMoveUp = { viewModel.moveItemUp(item) },
                                    onMoveDown = { viewModel.moveItemDown(item) },
                                    onPageRangeChange = { newTxt ->
                                        val idx = viewModel.items.indexOf(item)
                                        if (idx != -1) {
                                            viewModel.items[idx] = item.copy(pageSelectionText = newTxt)
                                            viewModel.resetSuccessStatesToPending()
                                        }
                                    },
                                    onInversionToggle = {
                                        val idx = viewModel.items.indexOf(item)
                                        if (idx != -1) {
                                            viewModel.items[idx] = item.copy(isInvertedSelection = !item.isInvertedSelection)
                                            viewModel.resetSuccessStatesToPending()
                                        }
                                    },
                                    onDpiChange = { newDpi ->
                                        val idx = viewModel.items.indexOf(item)
                                        if (idx != -1) {
                                            viewModel.items[idx] = item.copy(imageDpiSetting = newDpi)
                                            viewModel.resetSuccessStatesToPending()
                                        }
                                    },
                                    onRemove = {
                                        viewModel.removePdf(item)
                                    },
                                    onSaveAsClick = { uri ->
                                        onInitiateSave(uri, item.name)
                                    },
                                    onPrintClick = { uri ->
                                        printPdf(uri, item.name)
                                    },
                                    isGridView = isGridView
                                )
                            }
                            
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                androidx.compose.animation.AnimatedVisibility(visible = showExactPreview && viewModel.items.isNotEmpty()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Text("Output Preview", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, top = 12.dp))
                                            NUpPreviewPanel(
                                                totalOutputPages = viewModel.getExpectedPageCount(config),
                                                virtualPageSeq = viewModel.virtualPageSequenceGenerator.generateSequence(),
                                                config = config,
                                                cacheManager = viewModel.previewCacheManager,
                                                modifier = Modifier.fillMaxSize().padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.height(80.dp)) // FAB padding
                            }
                        }
                    }
                }
            }
        }
            
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            NUpKeyboardOverlay(
                isVisible = keyboardController.isVisible.value,
                onHide = {
                    keyboardController.hide()
                    focusManager.clearFocus()
                },
                onKeyPress = { key -> keyboardController.handleKeyPress(key) },
                isTablet = isTablet
            )
        }
        
        if (showHelpSheet) {
            NUpHelpSheet(
                onDismissRequest = { showHelpSheet = false },
                currentOrder = config.arrangementOrder
            )
        }

        if (showRenameDialog) {
            pdfToSaveUri?.let { uri ->
                SaveDestinationBottomSheet(
                    fileUri = uri,
                    defaultFileName = renameFileName,
                    onDismiss = { showRenameDialog = false },
                    onSuccess = { newName ->
                        renameFileName = newName
                    }
                )
            }
        }
    }
}
