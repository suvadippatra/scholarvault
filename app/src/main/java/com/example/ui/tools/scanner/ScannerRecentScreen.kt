package com.scholarvault.ui.tools.scanner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.scholarvault.data.model.ScannedDocumentEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerRecentScreen(
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    docViewModel: com.scholarvault.ui.viewmodel.DocumentViewModel
) {
    val context = LocalContext.current
    val scannerViewModel = getScannerViewModel(context)
    val recentScans by scannerViewModel.allScans.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isProcessing by scannerViewModel.isProcessing.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val filteredScans = remember(recentScans, searchQuery) {
        if (searchQuery.isBlank()) recentScans
        else recentScans.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
    }

    val prefs = context.getSharedPreferences("scanner_draft", android.content.Context.MODE_PRIVATE)
    var draftUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(Unit) {
        val savedDraftStr = prefs.getString("draft_uris", null)
        if (savedDraftStr != null) {
            try {
                val array = org.json.JSONArray(savedDraftStr)
                val uris = (0 until array.length()).map { Uri.parse(array.getString(it)) }
                draftUris = uris
            } catch (e: Exception) {}
        }
    }

    val draftItem = remember(draftUris) {
        if (draftUris.isNotEmpty()) {
            ScannedDocumentEntity(
                id = "draft_item",
                name = "Unsaved Draft",
                pagePaths = "[]",
                timestamp = java.util.Date(),
            )
        } else null
    }

    val displayScans = remember(filteredScans, draftItem) {
        if (draftItem != null && searchQuery.isBlank()) {
            listOf(draftItem) + filteredScans
        } else filteredScans
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToViewer("document_scanner_capture") }) {
                Icon(Icons.Default.Add, contentDescription = "Add New Scan")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            androidx.compose.animation.AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search scans...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                LaunchedEffect(showSearchBar) {
                    if (showSearchBar) focusRequester.requestFocus()
                }
            }

            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Processing scan in background...", 
                    modifier = Modifier.padding(16.dp), 
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (displayScans.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (recentScans.isEmpty()) "No recent scans found." else "No matches found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayScans) { scan ->
                            if (scan.id == "draft_item") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.7f)
                                        .clickable { onNavigateToViewer("document_scanner_capture") },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Draft", tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Unsaved Draft", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text("${draftUris.size} pages pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Resume", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            } else {
                                RecentScanCard(
                                    scan = scan,
                                    isGrid = true,
                                    onDelete = { scannerViewModel.deleteScan(scan) },
                                    onShare = { shareScan(context, scan) },
                                    onClick = { handleScanClick(scan, onNavigateToViewer) },
                                    onUpdateScan = { updatedScan -> scannerViewModel.updateScan(updatedScan) },
                                    onDownload = { downloadScan(context, scan) },
                                    onSaveToVault = { saveToVaultScan(context, coroutineScope, docViewModel, scan) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayScans) { scan ->
                            if (scan.id == "draft_item") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToViewer("document_scanner_capture") },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Draft", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Unsaved Draft", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            Text("${draftUris.size} pages pending. Tap to resume.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Resume", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            } else {
                                RecentScanCard(
                                    scan = scan,
                                    isGrid = false,
                                    onDelete = { scannerViewModel.deleteScan(scan) },
                                    onShare = { shareScan(context, scan) },
                                    onClick = { handleScanClick(scan, onNavigateToViewer) },
                                    onUpdateScan = { updatedScan -> scannerViewModel.updateScan(updatedScan) },
                                    onDownload = { downloadScan(context, scan) },
                                    onSaveToVault = { saveToVaultScan(context, coroutineScope, docViewModel, scan) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val newlyGeneratedScan by scannerViewModel.newlyGeneratedScan.collectAsState()
    if (newlyGeneratedScan != null) {
        val scan = newlyGeneratedScan!!
        var showRenameDialog by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf(scan.name) }
        
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Scan") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            scannerViewModel.updateScan(scan.copy(name = newName))
                            scannerViewModel.setNewlyGeneratedScan(scan.copy(name = newName))
                        }
                        showRenameDialog = false
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }

        ModalBottomSheet(onDismissRequest = { scannerViewModel.setNewlyGeneratedScan(null) }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                Text("Success!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                var fileSize = ""
                try {
                    val filePaths = org.json.JSONArray(scan.pagePaths)
                    if (filePaths.length() > 0) {
                        val file = File(filePaths.getString(0))
                        if(file.exists()) {
                            fileSize = "Exported: %.2f MB".format(file.length() / (1024f * 1024f))
                        }
                    }
                } catch(e: Exception){}
                
                Text(fileSize, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showRenameDialog = true }.padding(vertical = 8.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(scan.name, style = MaterialTheme.typography.bodyLarge)
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    shareScan(context, scan)
                }.padding(vertical = 12.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Share", style = MaterialTheme.typography.bodyLarge)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    downloadScan(context, scan)
                }.padding(vertical = 12.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Download, contentDescription = "Save to Device")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Save to Device", style = MaterialTheme.typography.bodyLarge)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    saveToVaultScan(context, coroutineScope, docViewModel, scan)
                }.padding(vertical = 12.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Save, contentDescription = "Save to Vault")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Save to Vault", style = MaterialTheme.typography.bodyLarge)
                }
                
                Button(onClick = { scannerViewModel.setNewlyGeneratedScan(null) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Done")
                }
            }
        }
    }
}

private fun shareScan(context: android.content.Context, scan: ScannedDocumentEntity) {
    try {
        val filePaths = org.json.JSONArray(scan.pagePaths)
        if (filePaths.length() > 0) {
            val firstFile = File(filePaths.getString(0))
            if (firstFile.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", firstFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Document"))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun handleScanClick(scan: ScannedDocumentEntity, onNavigateToViewer: (String) -> Unit) {
    try {
        val filePaths = org.json.JSONArray(scan.pagePaths)
        if (filePaths.length() > 0) {
            val firstFile = filePaths.getString(0)
            if (firstFile.endsWith(".pdf", ignoreCase = true)) {
                val encodePath = android.util.Base64.encodeToString(firstFile.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val encodeName = android.util.Base64.encodeToString(scan.name.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                onNavigateToViewer("viewer/pdf/$encodePath/$encodeName")
            } else {
                onNavigateToViewer("document_scanner_capture/${scan.id}")
            }
        }
    } catch (e: Exception) {}
}

private fun downloadScan(context: android.content.Context, scan: ScannedDocumentEntity) {
    try {
        val filePaths = org.json.JSONArray(scan.pagePaths)
        if (filePaths.length() > 0) {
            val firstFile = File(filePaths.getString(0))
            if (firstFile.exists()) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, "${scan.name}.pdf")
                firstFile.copyTo(destFile, overwrite = true)
                android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    } catch(e: Exception) {
        android.widget.Toast.makeText(context, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun saveToVaultScan(context: android.content.Context, coroutineScope: kotlinx.coroutines.CoroutineScope, docViewModel: com.scholarvault.ui.viewmodel.DocumentViewModel, scan: ScannedDocumentEntity) {
    try {
        val filePaths = org.json.JSONArray(scan.pagePaths)
        if (filePaths.length() > 0) {
            val firstFile = File(filePaths.getString(0))
            if (firstFile.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", firstFile)
                coroutineScope.launch {
                    docViewModel.importFiles(context, listOf(uri), null)
                    android.widget.Toast.makeText(context, "Saved to App Vault", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch(e: Exception) {
        android.widget.Toast.makeText(context, "Failed to save to vault", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RecentScanCard(
    scan: ScannedDocumentEntity,
    isGrid: Boolean = false,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit,
    onUpdateScan: (ScannedDocumentEntity) -> Unit,
    onDownload: () -> Unit,
    onSaveToVault: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(scan.name) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var currentTagsStr by remember { mutableStateOf(scan.tags.joinToString(", ")) }

    if (showTagsDialog) {
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            title = { Text("Edit Tags") },
            text = {
                OutlinedTextField(
                    value = currentTagsStr,
                    onValueChange = { currentTagsStr = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTags = currentTagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onUpdateScan(scan.copy(tags = newTags))
                    showTagsDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTagsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Scan") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onUpdateScan(scan.copy(name = newName))
                    }
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    var isDraft = false
    try {
        val filePaths = org.json.JSONArray(scan.pagePaths)
        if (filePaths.length() > 0) {
            isDraft = !filePaths.getString(0).endsWith(".pdf", ignoreCase = true)
        }
    } catch(e: Exception){}

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Box {
                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                            Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        ScanDropdownMenu(expanded, { expanded = false }, showRenameDialog = { showRenameDialog = true }, showTagsDialog = { showTagsDialog = true }, onShare, onSaveToVault, onDownload, onDelete, isDraft)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = scan.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                var fileSize = ""
                try {
                    val filePaths = org.json.JSONArray(scan.pagePaths)
                    if (filePaths.length() > 0) {
                        if (isDraft) {
                            fileSize = "${filePaths.length()} Draft Pages"
                        } else {
                            val file = File(filePaths.getString(0))
                            if (file.exists()) {
                                fileSize = "%.1f MB".format(file.length() / (1024f * 1024f))
                            }
                        }
                    }
                } catch(e: Exception){}
                Text(
                    text = SimpleDateFormat("MMM dd, yy", Locale.getDefault()).format(scan.timestamp) + " • " + fileSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (scan.tags.isNotEmpty()) {
                    Text("• ${scan.tags.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = scan.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    var fileSize = ""
                    try {
                        val filePaths = org.json.JSONArray(scan.pagePaths)
                        if (filePaths.length() > 0) {
                            if (isDraft) {
                                fileSize = " • ${filePaths.length()} Draft Pages"
                            } else {
                                val file = File(filePaths.getString(0))
                                if (file.exists()) {
                                    fileSize = " • PDF: %.2f MB".format(file.length() / (1024f * 1024f))
                                }
                            }
                        }
                    } catch(e: Exception){}

                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(scan.timestamp) + fileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (scan.tags.isNotEmpty()) {
                        Text("• ${scan.tags.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    ScanDropdownMenu(expanded, { expanded = false }, showRenameDialog = { showRenameDialog = true }, showTagsDialog = { showTagsDialog = true }, onShare, onSaveToVault, onDownload, onDelete, isDraft)
                }
            }
        }
    }
}

@Composable
private fun ScanDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showRenameDialog: () -> Unit,
    showTagsDialog: () -> Unit,
    onShare: () -> Unit,
    onSaveToVault: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    isDraft: Boolean
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = { onDismissRequest(); showRenameDialog() },
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Edit, null) }
        )
        DropdownMenuItem(
            text = { Text("Edit Tags") },
            onClick = { onDismissRequest(); showTagsDialog() },
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Add, null) }
        )
        if (!isDraft) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { onDismissRequest(); onShare() },
                leadingIcon = { Icon(Icons.Default.Share, null) }
            )
            DropdownMenuItem(
                text = { Text("Save to Vault") },
                onClick = { onDismissRequest(); onSaveToVault() },
                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Save, null) }
            )
            DropdownMenuItem(
                text = { Text("Save to Device") },
                onClick = { onDismissRequest(); onDownload() },
                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Download, null) }
            )
        }
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = { onDismissRequest(); onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

