package com.scholarvault.ui.tools.quick_note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.MainApplication
import com.scholarvault.data.model.QuickNoteEntity
import com.scholarvault.data.repository.QuickNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteFullScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val repository = remember { QuickNoteRepository(app.database.quickNoteDao()) }
    val allNotes by repository.getAllQuickNotes().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(QuickNoteMode.LIST) }
    var activeNote by remember { mutableStateOf<QuickNoteEntity?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // List view type: 0 = Flat list/grid, 1 = Folders
    var listTabState by remember { mutableStateOf(0) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }

    val filteredNotes = remember(allNotes, searchQuery) {
        if (searchQuery.isBlank()) allNotes
        else allNotes.filter { it.content.contains(searchQuery, ignoreCase = true) || it.folder?.contains(searchQuery, ignoreCase = true) == true || it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) } }
    }

    var viewZoom by remember { mutableFloatStateOf(1f) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        val text = activeNote?.content ?: ""
                        out.write(text.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val exportTxtLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        val text = activeNote?.content ?: ""
                        out.write(text.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val exportPdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        writeNoteToPdf(context, out, "Note Document", activeNote?.content ?: "")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when (mode) {
                    QuickNoteMode.LIST -> "Take Notes"
                    QuickNoteMode.VIEW -> "View Note"
                    QuickNoteMode.EDIT -> "Edit Note"
                }) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode == QuickNoteMode.LIST) {
                            if (selectedFolderFilter != null) {
                                selectedFolderFilter = null
                            } else {
                                onBack()
                            }
                        } else {
                            if (mode == QuickNoteMode.EDIT && activeNote?.content?.isNotBlank() == true) {
                                mode = QuickNoteMode.VIEW
                            } else {
                                mode = QuickNoteMode.LIST
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (mode == QuickNoteMode.LIST) {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    } else if (mode == QuickNoteMode.VIEW) {
                        IconButton(onClick = { viewZoom = (viewZoom - 0.1f).coerceAtLeast(0.5f) }) {
                            Text("A-", fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewZoom = (viewZoom + 0.1f).coerceAtMost(3f) }) {
                            Text("A+", fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { mode = QuickNoteMode.EDIT }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                        }
                        var showExportMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share or Export")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Share Plain Text") },
                                onClick = {
                                    showExportMenu = false
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, activeNote?.content)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as .txt file") },
                                onClick = {
                                    showExportMenu = false
                                    shareNoteAsFile(context, "Note", activeNote?.content ?: "", "txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as .md file") },
                                onClick = {
                                    showExportMenu = false
                                    shareNoteAsFile(context, "Note", activeNote?.content ?: "", "md")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as .pdf file") },
                                onClick = {
                                    showExportMenu = false
                                    shareNoteAsFile(context, "Note", activeNote?.content ?: "", "pdf")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Export as .txt doc") },
                                onClick = {
                                    showExportMenu = false
                                    exportTxtLauncher.launch("Note_${System.currentTimeMillis()}.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as .md doc") },
                                onClick = {
                                    showExportMenu = false
                                    exportLauncher.launch("Note_${System.currentTimeMillis()}.md")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as .pdf doc") },
                                onClick = {
                                    showExportMenu = false
                                    exportPdfLauncher.launch("Note_${System.currentTimeMillis()}.pdf")
                                }
                            )
                        }
                    } else if (mode == QuickNoteMode.EDIT) {
                        IconButton(onClick = {
                            if (activeNote?.content?.isNotBlank() == true) {
                                mode = QuickNoteMode.VIEW
                            } else {
                                mode = QuickNoteMode.LIST
                            }
                        }) {
                            Icon(Icons.Default.Visibility, contentDescription = "View Markdown")
                        }
                        IconButton(onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, activeNote?.content)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (mode == QuickNoteMode.LIST) {
                FloatingActionButton(onClick = {
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        }
    ) { paddingInfo ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingInfo)) {
            if (showCreateDialog) {
                var folderInput by remember { mutableStateOf("") }
                var tagsInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Note") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Set folder and tags (optional):", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = folderInput,
                                onValueChange = { folderInput = it },
                                label = { Text("Folder Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = tagsInput,
                                onValueChange = { tagsInput = it },
                                label = { Text("Tags (comma separated)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val parsedFolder = folderInput.trim().ifEmpty { null }
                            activeNote = QuickNoteEntity(content = "", folder = parsedFolder, tags = parsedTags)
                            showCreateDialog = false
                            mode = QuickNoteMode.EDIT
                        }) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (mode == QuickNoteMode.LIST) {
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.animation.AnimatedVisibility(visible = showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search notes...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        LaunchedEffect(showSearchBar) {
                            if (showSearchBar) focusRequester.requestFocus()
                        }
                    }

                    TabRow(selectedTabIndex = listTabState) {
                        Tab(
                            selected = listTabState == 0,
                            onClick = { 
                                listTabState = 0 
                                selectedFolderFilter = null
                            },
                            text = { Text("All Notes") }
                        )
                        Tab(
                            selected = listTabState == 1,
                            onClick = { listTabState = 1 },
                            text = { Text("Folder View") }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (listTabState == 1 && selectedFolderFilter == null) {
                        // Display folders
                        val folderMap = remember(filteredNotes) {
                            filteredNotes.groupBy { it.folder ?: "Uncategorized" }
                        }

                        if (folderMap.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No folders found. Create a note and assign a folder.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                folderMap.forEach { (name, notesList) ->
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedFolderFilter = name },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = "Folder Icon",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${notesList.size} notes",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Display list of notes (all or in a specific folder)
                        val notesToDisplay = remember(filteredNotes, selectedFolderFilter, listTabState) {
                            if (listTabState == 1 && selectedFolderFilter != null) {
                                filteredNotes.filter { (it.folder ?: "Uncategorized") == selectedFolderFilter }
                            } else {
                                filteredNotes
                            }
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (listTabState == 1 && selectedFolderFilter != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFolderFilter = null }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Folders",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Folders / $selectedFolderFilter",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (notesToDisplay.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(if (allNotes.isEmpty()) "No quick notes. Tap + to add." else "No notes match search.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
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
                                        items(notesToDisplay) { note ->
                                            QuickNoteCardFullScreen(
                                                note = note,
                                                isGrid = true,
                                                onClick = { 
                                                    activeNote = note
                                                    mode = QuickNoteMode.VIEW
                                                },
                                                onEdit = {
                                                    activeNote = note
                                                    mode = QuickNoteMode.EDIT
                                                },
                                                onDelete = {
                                                    coroutineScope.launch(Dispatchers.IO) { repository.deleteQuickNote(note) }
                                                },
                                                context = context
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(notesToDisplay) { note ->
                                            QuickNoteCardFullScreen(
                                                note = note,
                                                isGrid = false,
                                                onClick = { 
                                                    activeNote = note
                                                    mode = QuickNoteMode.VIEW
                                                },
                                                onEdit = {
                                                    activeNote = note
                                                    mode = QuickNoteMode.EDIT
                                                },
                                                onDelete = {
                                                    coroutineScope.launch(Dispatchers.IO) { repository.deleteQuickNote(note) }
                                                },
                                                context = context
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (mode == QuickNoteMode.VIEW) {
                activeNote?.let { note ->
                    val scrollState = rememberScrollState()
                    var zoomScale by remember { mutableFloatStateOf(1f) }
                    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 4.0f)
                        zoomOffset = Offset(
                            x = (zoomOffset.x + offsetChange.x).coerceIn(-1000f, 1000f),
                            y = (zoomOffset.y + offsetChange.y).coerceIn(-1000f, 1000f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale * viewZoom,
                                    scaleY = zoomScale * viewZoom,
                                    translationX = zoomOffset.x,
                                    translationY = zoomOffset.y
                               )
                        ) {
                            Column(modifier = Modifier.fillMaxHeight().widthIn(max = 800.dp).padding(16.dp).verticalScroll(scrollState)) {
                                com.scholarvault.ui.components.SimpleMarkdownText(text = note.content)
                            }
                        }
                    }
                }
            } else if (mode == QuickNoteMode.EDIT) {
                activeNote?.let { note ->
                    var zoomScale by remember { mutableFloatStateOf(1f) }
                    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 4.0f)
                        zoomOffset = Offset(
                            x = (zoomOffset.x + offsetChange.x).coerceIn(-1000f, 1000f),
                            y = (zoomOffset.y + offsetChange.y).coerceIn(-1000f, 1000f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = zoomOffset.x,
                                    translationY = zoomOffset.y
                                )
                        ) {
                            QuickNoteEditor(
                                note = note,
                                repository = repository,
                                onContentChanged = { updatedNote -> activeNote = updatedNote },
                                showFormatting = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickNoteCardFullScreen(
    note: QuickNoteEntity,
    isGrid: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    context: android.content.Context
) {
    var expanded by remember { mutableStateOf(false) }

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
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.content.ifEmpty { "Empty Note" },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        NoteDropdownFullScreen(expanded, onDismiss = { expanded = false }, note, onEdit = onEdit, onDelete = onDelete, context = context)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val dateStr = android.text.format.DateFormat.format("MMM dd, yy", note.timestamp).toString()
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (note.folder != null) {
                    Text("• ${note.folder}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (note.tags.isNotEmpty()) {
                    Text("• ${note.tags.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.content.ifEmpty { "Empty Note" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = android.text.format.DateFormat.format("MMM dd, yyyy", note.timestamp).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (note.folder != null) {
                            Text(" • ${note.folder}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        if (note.tags.isNotEmpty()) {
                            Text(" • ${note.tags.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    NoteDropdownFullScreen(expanded, onDismiss = { expanded = false }, note, onEdit = onEdit, onDelete = onDelete, context = context)
                }
            }
        }
    }
}

@Composable
private fun NoteDropdownFullScreen(
    expanded: Boolean,
    onDismiss: () -> Unit,
    note: QuickNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    context: android.content.Context
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onDismiss()
                onEdit()
            },
            leadingIcon = { Icon(Icons.Default.Edit, null) }
        )
        DropdownMenuItem(
            text = { Text("Share") },
            onClick = {
                onDismiss()
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, note.content)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
            },
            leadingIcon = { Icon(Icons.Default.Share, null) }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

// PDF EXPORT HELPER METHODS
fun writeNoteToPdf(context: android.content.Context, outputStream: OutputStream, noteTitle: String, noteContent: String) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    val pageWidth = 595 // A4 standard width in points
    val pageHeight = 842 // A4 standard height in points
    val marginLeft = 54f
    val marginRight = 54f
    val marginTop = 54f
    val marginBottom = 54f
    val contentWidth = pageWidth - marginLeft - marginRight
    
    var pageNumber = 1
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    
    // Draw Title Header
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 20f
    paint.color = android.graphics.Color.BLACK
    canvas.drawText(noteTitle, marginLeft, marginTop + 20f, paint)
    
    // Draw Divider Line
    paint.strokeWidth = 1f
    paint.color = android.graphics.Color.GRAY
    canvas.drawLine(marginLeft, marginTop + 35f, pageWidth - marginRight, marginTop + 35f, paint)
    
    // Setup Paint for body text
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.textSize = 12f
    paint.color = android.graphics.Color.BLACK
    val fontMetrics = paint.fontMetrics
    val lineHeight = fontMetrics.bottom - fontMetrics.top + 4f
    
    var yPos = marginTop + 60f
    val paragraphs = noteContent.split("\n")
    val mediaPattern = Regex("""!\[(.*?)\]\((.*?)\)""")
    
    for (paragraph in paragraphs) {
        val matches = mediaPattern.findAll(paragraph).toList()
        if (matches.isEmpty()) {
            // Standard paragraph: just draw the text
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                val width = paint.measureText(testLine)
                if (width > contentWidth) {
                    // Check page limit
                    if (yPos + lineHeight > pageHeight - marginBottom) {
                        pdfDocument.finishPage(currentPage)
                        pageNumber++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        yPos = marginTop
                    }
                    canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                    yPos += lineHeight
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }
            
            if (currentLine.isNotEmpty()) {
                if (yPos + lineHeight > pageHeight - marginBottom) {
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    yPos = marginTop
                }
                canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                yPos += lineHeight
            }
            yPos += 8f // paragraph gap
        } else {
            // Line with attachment markup (Image or Audio)
            var lastIdx = 0
            for (match in matches) {
                val preText = paragraph.substring(lastIdx, match.range.first).trim()
                if (preText.isNotEmpty()) {
                    val words = preText.split(" ")
                    var currentLine = StringBuilder()
                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                        val width = paint.measureText(testLine)
                        if (width > contentWidth) {
                            if (yPos + lineHeight > pageHeight - marginBottom) {
                                pdfDocument.finishPage(currentPage)
                                pageNumber++
                                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                                currentPage = pdfDocument.startPage(pageInfo)
                                canvas = currentPage.canvas
                                yPos = marginTop
                            }
                            canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                            yPos += lineHeight
                            currentLine = StringBuilder(word)
                        } else {
                            currentLine = StringBuilder(testLine)
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        if (yPos + lineHeight > pageHeight - marginBottom) {
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yPos = marginTop
                        }
                        canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                        yPos += lineHeight
                    }
                }

                val altText = match.groupValues[1]
                val uriStr = match.groupValues[2]

                if (altText.equals("Audio", ignoreCase = true) || uriStr.contains("audio") || uriStr.endsWith(".mp3") || uriStr.endsWith(".m4a") || uriStr.endsWith(".wav")) {
                    val cardHeight = 40f
                    if (yPos + cardHeight > pageHeight - marginBottom) {
                        pdfDocument.finishPage(currentPage)
                        pageNumber++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        yPos = marginTop
                    }

                    // Draw placeholder card
                    paint.color = android.graphics.Color.LTGRAY
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRect(marginLeft, yPos, pageWidth - marginRight, yPos + cardHeight, paint)
                    
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.DKGRAY
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                    canvas.drawText("🎵 Audio Attachment: $altText", marginLeft + 10f, yPos + 24f, paint)

                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.color = android.graphics.Color.BLACK
                    yPos += cardHeight + 12f
                } else {
                    // Render Image
                    val bitmap = try {
                        if (uriStr.startsWith("/")) {
                            android.graphics.BitmapFactory.decodeFile(uriStr)
                        } else {
                            context.contentResolver.openInputStream(android.net.Uri.parse(uriStr))?.use {
                                android.graphics.BitmapFactory.decodeStream(it)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    if (bitmap != null) {
                        val origW = bitmap.width.toFloat()
                        val origH = bitmap.height.toFloat()
                        val targetW = contentWidth
                        val targetH = (origH / origW) * targetW

                        if (yPos + targetH > pageHeight - marginBottom) {
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yPos = marginTop
                        }

                        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                        val dstRect = android.graphics.RectF(marginLeft, yPos, marginLeft + targetW, yPos + targetH)
                        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                        yPos += targetH + 12f
                        bitmap.recycle()
                    } else {
                        val placeholderH = 30f
                        if (yPos + placeholderH > pageHeight - marginBottom) {
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yPos = marginTop
                        }
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.color = android.graphics.Color.RED
                        canvas.drawRect(marginLeft, yPos, pageWidth - marginRight, yPos + placeholderH, paint)
                        
                        paint.style = android.graphics.Paint.Style.FILL
                        paint.color = android.graphics.Color.BLACK
                        canvas.drawText("📷 Image File: $uriStr", marginLeft + 10f, yPos + 18f, paint)
                        yPos += placeholderH + 12f
                    }
                }
                lastIdx = match.range.last + 1
            }

            val postText = paragraph.substring(lastIdx).trim()
            if (postText.isNotEmpty()) {
                val words = postText.split(" ")
                var currentLine = StringBuilder()
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                    val width = paint.measureText(testLine)
                    if (width > contentWidth) {
                        if (yPos + lineHeight > pageHeight - marginBottom) {
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yPos = marginTop
                        }
                        canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                        yPos += lineHeight
                        currentLine = StringBuilder(word)
                    } else {
                        currentLine = StringBuilder(testLine)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    if (yPos + lineHeight > pageHeight - marginBottom) {
                        pdfDocument.finishPage(currentPage)
                        pageNumber++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        yPos = marginTop
                    }
                    canvas.drawText(currentLine.toString(), marginLeft, yPos, paint)
                    yPos += lineHeight
                }
                yPos += 8f
            }
        }
    }
    
    pdfDocument.finishPage(currentPage)
    try {
        pdfDocument.writeTo(outputStream)
    } finally {
        pdfDocument.close()
    }
}

fun shareNoteAsFile(
    context: android.content.Context,
    title: String,
    content: String,
    extension: String
) {
    val cachePath = File(context.cacheDir, "Shared_Notes")
    cachePath.mkdirs()
    val noteTitle = if (title.isBlank()) "Note" else title.replace(" ", "_")
    val file = File(cachePath, "${noteTitle}_${System.currentTimeMillis()}.$extension")
    
    try {
        if (extension == "pdf") {
            FileOutputStream(file).use { out ->
                writeNoteToPdf(context, out, "Note Document", content)
            }
        } else {
            file.writeText(content)
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val mimeType = when (extension) {
            "pdf" -> "application/pdf"
            "md" -> "text/markdown"
            else -> "text/plain"
        }
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share note as $extension"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
