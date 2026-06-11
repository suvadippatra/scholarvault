@file:OptIn(ExperimentalMaterial3Api::class)

package com.scholarvault.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.launch
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintAttributes
import android.content.Context
import java.io.FileInputStream
import java.io.FileOutputStream
import android.os.ParcelFileDescriptor
import android.os.CancellationSignal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrePrintingSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val app = context.applicationContext as com.scholarvault.MainApplication
    val docRepository = remember { com.scholarvault.data.repository.DocumentRepository(app.database.documentDao(), app.database.walletDao()) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSaveAppFolderDialog by remember { mutableStateOf(false) }
    var showSaveDownloadsDialog by remember { mutableStateOf(false) }
    var saveFileName by remember { mutableStateOf("Printed_Document.pdf") }
    var saveTags by remember { mutableStateOf("Printed, N-Up") }
    var currentlyProcessedFile by remember { mutableStateOf<java.io.File?>(null) }
    
    val sharedPref = remember { context.getSharedPreferences("PrintSetupPrefs", Context.MODE_PRIVATE) }
    
    var items by remember { mutableStateOf<List<PrintJobItem>>(emptyList()) }
    var pagesPerSheet by remember { mutableStateOf(sharedPref.getInt("pagesPerSheet", 1)) }
    var keepBorder by remember { mutableStateOf(sharedPref.getBoolean("keepBorder", true)) }
    var pageMarginMm by remember { mutableStateOf(sharedPref.getString("pageMarginMm", "") ?: "") }
    var pageGapMm by remember { mutableStateOf(sharedPref.getString("pageGapMm", "") ?: "") }
    var bookletMode by remember { mutableStateOf(sharedPref.getBoolean("bookletMode", false)) }
    var pageFit by remember {
        val savedName = sharedPref.getString("pageFit", PdfNUpProcessor.PageFit.FIT_BEST.name)
        mutableStateOf(PdfNUpProcessor.PageFit.values().firstOrNull { it.name == savedName } ?: PdfNUpProcessor.PageFit.FIT_BEST)
    }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(pagesPerSheet, keepBorder, pageMarginMm, pageGapMm, bookletMode, pageFit) {
        sharedPref.edit().apply {
            putInt("pagesPerSheet", pagesPerSheet)
            putBoolean("keepBorder", keepBorder)
            putString("pageMarginMm", pageMarginMm)
            putString("pageGapMm", pageGapMm)
            putBoolean("bookletMode", bookletMode)
            putString("pageFit", pageFit.name)
            apply()
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 840
    var showDropBox by remember { mutableStateOf(isLargeScreen) }

    LaunchedEffect(Unit) {
        if (SharedData.pendingUris.value.isNotEmpty()) {
            val newItems = mutableListOf<PrintJobItem>()
            for (uri in SharedData.pendingUris.value) {
                newItems.add(PrintJobUtils.createPrintJobItem(context, uri))
            }
            items = items + newItems
            SharedData.pendingUris.value = emptyList()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { result ->
        if (result.isNotEmpty()) {
            coroutineScope.launch {
                val newItems = mutableListOf<PrintJobItem>()
                for (uri in result) {
                    newItems.add(PrintJobUtils.createPrintJobItem(context, uri))
                }
                items = items + newItems
            }
        }
    }

    // --- Save to App Folder Dialog ---
    if (showSaveAppFolderDialog && currentlyProcessedFile != null) {
        val pdfFile = currentlyProcessedFile!!
        AlertDialog(
            onDismissRequest = { showSaveAppFolderDialog = false },
            title = { Text("Save to My Files") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Register this compiled PDF into the app's secure files explorer.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveTags,
                        onValueChange = { saveTags = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetName = saveFileName
                    val targetTags = saveTags
                    showSaveAppFolderDialog = false
                    coroutineScope.launch {
                        val success = savePdfToAppFolder(context, pdfFile, targetName, targetTags, docRepository)
                        if (success) {
                            snackbarHostState.showSnackbar("Saved compiled PDF to My Files!")
                        } else {
                            snackbarHostState.showSnackbar("Save failed.")
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAppFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Save to Downloads Dialog ---
    if (showSaveDownloadsDialog && currentlyProcessedFile != null) {
        val pdfFile = currentlyProcessedFile!!
        AlertDialog(
            onDismissRequest = { showSaveDownloadsDialog = false },
            title = { Text("Save to Downloads") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Save compiled PDF to public Downloads folder.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetName = saveFileName
                    showSaveDownloadsDialog = false
                    coroutineScope.launch {
                        val finalUri = savePdfToDownloads(context, pdfFile, targetName)
                        if (finalUri != null) {
                            snackbarHostState.showSnackbar("Saved to Downloads!")
                        } else {
                            snackbarHostState.showSnackbar("Save failed.")
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDownloadsDialog = false }) {
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
                title = "Pre-Printing Setup",
                showProfileIcon = false,
                showSearchBar = false,
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu, 
                            onDismissRequest = { showMenu = false },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(text = { Text("Clear All") }, onClick = { items = emptyList(); showMenu = false })
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (!isLargeScreen && !showDropBox) {
                    AddFilesMenuButton(
                        onDeviceClick = { filePicker.launch(arrayOf("application/pdf", "image/*")) },
                        onUrisSelected = { uris ->
                            coroutineScope.launch {
                                val newItems = uris.map { PrintJobUtils.createPrintJobItem(context, it) }
                                items = items + newItems
                            }
                        },
                        onLongPress = { showDropBox = true },
                        label = "Add Files"
                    )
                }
                Spacer(Modifier.weight(1f))
                
                // Print & Save button
                var expandedPrintMenu by remember { mutableStateOf(false) }

                Box {
                    FloatingActionButton(
                        onClick = {
                            if (items.isNotEmpty()) {
                                expandedPrintMenu = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Print, "Print & Save")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Print & Save")
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPrintMenu,
                        onDismissRequest = { expandedPrintMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Direct Print") },
                            onClick = {
                                expandedPrintMenu = false
                                coroutineScope.launch {
                                    isProcessing = true
                                    try {
                                        val outPdf = PdfNUpProcessor.processFiles(
                                            context = context,
                                            items = items,
                                            pagesPerSheet = pagesPerSheet,
                                            keepBorder = keepBorder,
                                            pageSize = PDRectangle.A4,
                                            marginMm = pageMarginMm.toFloatOrNull() ?: 0f,
                                            gapMm = pageGapMm.toFloatOrNull() ?: 0f,
                                            bookletMode = bookletMode,
                                            pageFit = pageFit
                                        )
                                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                        val jobName = "Document_${System.currentTimeMillis()}"
                                        printManager.print(jobName, object : PrintDocumentAdapter() {
                                            override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.LayoutResultCallback?, extras: android.os.Bundle?) {
                                                if (cancellationSignal?.isCanceled == true) {
                                                    callback?.onLayoutCancelled()
                                                    return
                                                }
                                                val info = android.print.PrintDocumentInfo.Builder(jobName).setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
                                                callback?.onLayoutFinished(info, true)
                                            }
                                            override fun onWrite(pages: Array<out android.print.PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.WriteResultCallback?) {
                                                var inStream: FileInputStream? = null
                                                var outStream: FileOutputStream? = null
                                                try {
                                                    inStream = FileInputStream(outPdf)
                                                    outStream = FileOutputStream(destination?.fileDescriptor)
                                                    val buf = ByteArray(16384)
                                                    var size: Int
                                                    while (inStream.read(buf).also { size = it } >= 0) outStream.write(buf, 0, size)
                                                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                                } catch (e: Exception) {
                                                    callback?.onWriteFailed(e.message)
                                                } finally {
                                                    inStream?.close()
                                                    outStream?.close()
                                                }
                                            }
                                        }, null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save to Secure Files (My Files)") },
                            onClick = {
                                expandedPrintMenu = false
                                coroutineScope.launch {
                                    isProcessing = true
                                    try {
                                        val outPdf = PdfNUpProcessor.processFiles(
                                            context = context,
                                            items = items,
                                            pagesPerSheet = pagesPerSheet,
                                            keepBorder = keepBorder,
                                            pageSize = PDRectangle.A4,
                                            marginMm = pageMarginMm.toFloatOrNull() ?: 0f,
                                            gapMm = pageGapMm.toFloatOrNull() ?: 0f,
                                            bookletMode = bookletMode,
                                            pageFit = pageFit
                                        )
                                        currentlyProcessedFile = outPdf
                                        saveFileName = "Printed_NUp_${System.currentTimeMillis()}.pdf"
                                        showSaveAppFolderDialog = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save to Device folder (Downloads)") },
                            onClick = {
                                expandedPrintMenu = false
                                coroutineScope.launch {
                                    isProcessing = true
                                    try {
                                        val outPdf = PdfNUpProcessor.processFiles(
                                            context = context,
                                            items = items,
                                            pagesPerSheet = pagesPerSheet,
                                            keepBorder = keepBorder,
                                            pageSize = PDRectangle.A4,
                                            marginMm = pageMarginMm.toFloatOrNull() ?: 0f,
                                            gapMm = pageGapMm.toFloatOrNull() ?: 0f,
                                            bookletMode = bookletMode,
                                            pageFit = pageFit
                                        )
                                        currentlyProcessedFile = outPdf
                                        saveFileName = "Printed_NUp_${System.currentTimeMillis()}.pdf"
                                        showSaveDownloadsDialog = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val contentModifier = Modifier.fillMaxSize()
            if (isLargeScreen) {
                // Desktop / Tablet Layout
                Row(modifier = contentModifier) {
                    // Left Panel: Options and Items
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        OptionsAndItemsPanel(
                            items = items,
                            onItemsChanged = { items = it },
                            showDropBox = showDropBox,
                            onShowDropBoxChanged = { showDropBox = it },
                            pagesPerSheet = pagesPerSheet,
                            onPagesPerSheetChanged = { pagesPerSheet = it },
                            pageFit = pageFit,
                            onPageFitChanged = { pageFit = it },
                            pageMarginMm = pageMarginMm,
                            onPageMarginMmChanged = { pageMarginMm = it },
                            pageGapMm = pageGapMm,
                            onPageGapMmChanged = { pageGapMm = it },
                            keepBorder = keepBorder,
                            onKeepBorderChanged = { keepBorder = it },
                            bookletMode = bookletMode,
                            onBookletModeChanged = { bookletMode = it }
                        )
                    }
                    
                    // Vertical Divider
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.LightGray))
                    
                    // Right Panel: Preview
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                        PreviewPanel(
                            items = items,
                            pagesPerSheet = pagesPerSheet,
                            keepBorder = keepBorder,
                            pageMarginMm = pageMarginMm,
                            pageGapMm = pageGapMm,
                            bookletMode = bookletMode,
                            pageFit = pageFit
                        )
                    }
                }
            } else {
                // Mobile Layout
                LazyColumn(
                    modifier = contentModifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        OptionsAndItemsPanel(
                            items = items,
                            onItemsChanged = { items = it },
                            showDropBox = showDropBox,
                            onShowDropBoxChanged = { showDropBox = it },
                            pagesPerSheet = pagesPerSheet,
                            onPagesPerSheetChanged = { pagesPerSheet = it },
                            pageFit = pageFit,
                            onPageFitChanged = { pageFit = it },
                            pageMarginMm = pageMarginMm,
                            onPageMarginMmChanged = { pageMarginMm = it },
                            pageGapMm = pageGapMm,
                            onPageGapMmChanged = { pageGapMm = it },
                            keepBorder = keepBorder,
                            onKeepBorderChanged = { keepBorder = it },
                            bookletMode = bookletMode,
                            onBookletModeChanged = { bookletMode = it }
                        )
                    }
                    item {
                        PreviewPanel(
                            items = items,
                            pagesPerSheet = pagesPerSheet,
                            keepBorder = keepBorder,
                            pageMarginMm = pageMarginMm,
                            pageGapMm = pageGapMm,
                            bookletMode = bookletMode,
                            pageFit = pageFit
                        )
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun OptionsAndItemsPanel(
    items: List<PrintJobItem>,
    onItemsChanged: (List<PrintJobItem>) -> Unit,
    showDropBox: Boolean,
    onShowDropBoxChanged: (Boolean) -> Unit,
    pagesPerSheet: Int,
    onPagesPerSheetChanged: (Int) -> Unit,
    pageFit: PdfNUpProcessor.PageFit,
    onPageFitChanged: (PdfNUpProcessor.PageFit) -> Unit,
    pageMarginMm: String,
    onPageMarginMmChanged: (String) -> Unit,
    pageGapMm: String,
    onPageGapMmChanged: (String) -> Unit,
    keepBorder: Boolean,
    onKeepBorderChanged: (Boolean) -> Unit,
    bookletMode: Boolean,
    onBookletModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (showDropBox) {
            FileDropBox(
                onUrisSelected = { uris ->
                    coroutineScope.launch {
                        val newItems = mutableListOf<PrintJobItem>()
                        for (uri in uris) {
                            newItems.add(PrintJobUtils.createPrintJobItem(context, uri))
                        }
                        onItemsChanged(items + newItems)
                        onShowDropBoxChanged(false)
                    }
                },
                onClose = { onShowDropBoxChanged(false) },
                mimeType = "application/pdf"
            )
            Spacer(Modifier.height(8.dp))
        }
        
        if (items.isNotEmpty()) {
            Text("Layout Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pages per sheet: ", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                listOf(1, 2, 4, 6, 9).forEach { count ->
                    FilterChip(
                        selected = pagesPerSheet == count,
                        onClick = { onPagesPerSheetChanged(count) },
                        label = { Text("$count") },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                Text("Page Fit: ", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                PdfNUpProcessor.PageFit.values().forEach { fit ->
                    FilterChip(
                        selected = pageFit == fit,
                        onClick = { onPageFitChanged(fit) },
                        label = { Text(fit.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pageMarginMm,
                    onValueChange = { onPageMarginMmChanged(it) },
                    label = { Text("Margin (mm)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
                OutlinedTextField(
                    value = pageGapMm,
                    onValueChange = { onPageGapMmChanged(it) },
                    label = { Text("Gap (mm)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = keepBorder, onCheckedChange = { onKeepBorderChanged(it) })
                    Text("Draw page border", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bookletMode, onCheckedChange = { onBookletModeChanged(it) })
                    Text("Booklet Mode", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Text("Selected Files (${items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            items.forEachIndexed { index, item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${item.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onItemsChanged(items.toMutableList().apply { removeAt(index) })
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaPreviewThumbnail(item)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (item.isImage) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Output DPI: ", style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(
                                    value = item.outputDpi.toString(),
                                    onValueChange = { newVal ->
                                        val newDpi = newVal.toIntOrNull() ?: item.outputDpi
                                        onItemsChanged(items.toMutableList().apply { 
                                            this[index] = item.copy(outputDpi = newDpi) 
                                        })
                                    },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    modifier = Modifier.width(80.dp).height(48.dp)
                                )
                                Text("(Original: ${item.originalDpi} DPI)", fontSize = 10.sp, color = Color.Gray)
                            }
                        } else {
                            OutlinedTextField(
                                value = item.pageSequence,
                                onValueChange = { newSeq ->
                                    onItemsChanged(items.toMutableList().apply { this[index] = item.copy(pageSequence = newSeq) })
                                },
                                label = { Text("Pages to extract (e.g., 1,3-5)", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                            )
                        }
                        
                        SpaceSpacer(height = 4.dp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        onItemsChanged(items.toMutableList().apply {
                                            val temp = this[index - 1]
                                            this[index - 1] = this[index]
                                            this[index] = temp
                                        })
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move Up")
                            }
                            IconButton(
                                onClick = {
                                    if (index < items.size - 1) {
                                        onItemsChanged(items.toMutableList().apply {
                                            val temp = this[index + 1]
                                            this[index + 1] = this[index]
                                            this[index] = temp
                                        })
                                    }
                                },
                                enabled = index < items.size - 1,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move Down")
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No files selected. Add files or long press add button to drag and drop.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun PreviewPanel(
    items: List<PrintJobItem>,
    pagesPerSheet: Int,
    keepBorder: Boolean,
    pageMarginMm: String,
    pageGapMm: String,
    bookletMode: Boolean,
    pageFit: PdfNUpProcessor.PageFit
) {
    if (items.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Live Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FastComposePreview(
                items = items,
                pagesPerSheet = pagesPerSheet,
                keepBorder = keepBorder,
                pageMarginMm = pageMarginMm.toFloatOrNull() ?: 0f,
                pageGapMm = pageGapMm.toFloatOrNull() ?: 0f,
                bookletMode = bookletMode,
                pageFit = pageFit
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SpaceSpacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}

suspend fun savePdfToAppFolder(
    context: Context,
    pdfFile: java.io.File,
    desiredFileName: String,
    tagsCommaSeparated: String,
    repository: com.scholarvault.data.repository.DocumentRepository
): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        var outName = desiredFileName.trim()
        if (outName.isEmpty()) outName = "NUp_Printed_Document.pdf"
        if (!outName.lowercase().endsWith(".pdf")) outName += ".pdf"
        
        outName = outName.replace("/", "_").replace("\\", "_")
        
        val vaultFileName = "${System.currentTimeMillis()}_$outName"
        val vault = com.scholarvault.util.SecurityVault(context)
        val sizeBytes = pdfFile.length()
        val result = java.io.FileInputStream(pdfFile).use { input ->
            vault.saveEncryptedFileFromStream(vaultFileName, input)
        }
        val sandboxedFile = result.getOrThrow()
        
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

suspend fun savePdfToDownloads(context: Context, pdfFile: java.io.File, desiredFileName: String): Uri? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        var outName = desiredFileName.trim()
        if (outName.isEmpty()) outName = "NUp_Printed_Document.pdf"
        if (!outName.lowercase().endsWith(".pdf")) outName += ".pdf"
        
        outName = outName.replace("/", "_").replace("\\", "_")

        var finalUri: Uri? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, outName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/ScholarVault/Printed")
            }
            finalUri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            finalUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    java.io.FileInputStream(pdfFile).use { input ->
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
            val destDir = java.io.File(downloadDir, "ScholarVault/Printed")
            if (!destDir.exists()) destDir.mkdirs()
            
            var outFile = java.io.File(destDir, outName)
            var counter = 1
            while (outFile.exists()) {
                val base = outName.substringBeforeLast(".")
                val ext = outName.substringAfterLast(".", "pdf")
                outFile = java.io.File(destDir, "${base}_($counter).$ext")
                counter++
            }
            
            java.io.FileOutputStream(outFile).use { out ->
                java.io.FileInputStream(pdfFile).use { input ->
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
