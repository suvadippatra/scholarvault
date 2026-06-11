package com.scholarvault.ui.tools.quick_note

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.MainApplication
import com.scholarvault.data.model.QuickNoteEntity
import com.scholarvault.data.repository.QuickNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun QuickNoteWidgetOverlay(
    isWidgetVisible: Boolean,
    onCloseWidget: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        val app = context.applicationContext as MainApplication
        QuickNoteRepository(app.database.quickNoteDao())
    }

    if (isWidgetVisible) {
        QuickNoteDraggableContainer(repository = repository, onCloseWidget = onCloseWidget)
    }
}

@Composable
fun QuickNoteDraggableContainer(
    repository: QuickNoteRepository,
    onCloseWidget: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var isExpanded by remember { mutableStateOf(false) }
    
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            offsetX += dragAmount.x
            offsetY += dragAmount.y
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 120.dp) // initial position roughly
        ) {
            if (isExpanded) {
                QuickNoteExpandedPanel(
                    repository = repository,
                    onCloseClick = { isExpanded = false },
                    dragModifier = dragModifier
                )
            } else {
                QuickNoteFloatingBubble(
                    onClick = { isExpanded = true },
                    onCloseClick = onCloseWidget,
                    dragModifier = dragModifier
                )
            }
        }
    }
}

@Composable
fun QuickNoteFloatingBubble(onClick: () -> Unit, onCloseClick: () -> Unit, dragModifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(60.dp)
            .then(dragModifier)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.NoteAlt, contentDescription = "Take Notes", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            
            // small close button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Widget", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun QuickNoteExpandedPanel(repository: QuickNoteRepository, onCloseClick: () -> Unit, dragModifier: Modifier = Modifier, modifier: Modifier = Modifier.width(320.dp).height(400.dp), isFullScreen: Boolean = false) {
    var mode by remember { mutableStateOf(QuickNoteMode.LIST) }
    var activeNote by remember { mutableStateOf<QuickNoteEntity?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val allNotes by repository.getAllQuickNotes().collectAsState(initial = emptyList())
    
    var showCreateDialog by remember { mutableStateOf(false) }

    // For markdown export 
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        val content = allNotes.joinToString("\n\n---\n\n") { note ->
                            "## Note from ${note.timestamp}\n\n${note.content}"
                        }
                        out.write(content.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showCreateDialog) {
        var folderInput by remember { mutableStateOf("") }
        var tagsInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Note", fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set folder and tags (optional):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = folderInput, onValueChange = { folderInput = it }, label = { Text("Folder Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagsInput, onValueChange = { tagsInput = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val parsedFolder = folderInput.trim().ifEmpty { null }
                    activeNote = QuickNoteEntity(content = "", folder = parsedFolder, tags = parsedTags)
                    showCreateDialog = false
                    mode = QuickNoteMode.EDIT
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(dragModifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val title = when(mode) {
                    QuickNoteMode.LIST -> "Take Notes"
                    QuickNoteMode.EDIT -> "Edit Note"
                    QuickNoteMode.VIEW -> "View Note"
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp
                )
                Row {
                    if (mode == QuickNoteMode.LIST) {
                        IconButton(onClick = { exportLauncher.launch("QuickNotes_Backup.md") }) {
                            Icon(Icons.Default.Download, contentDescription = "Export All", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        IconButton(onClick = { 
                            showCreateDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Note", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    } else if (mode == QuickNoteMode.VIEW) {
                        IconButton(onClick = { mode = QuickNoteMode.EDIT }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        IconButton(onClick = { mode = QuickNoteMode.LIST }) {
                            Icon(Icons.Default.Close, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    } else {
                        IconButton(onClick = { mode = if (activeNote?.content?.isNotBlank() == true) QuickNoteMode.VIEW else QuickNoteMode.LIST }) {
                            Icon(Icons.Default.Close, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    IconButton(onClick = onCloseClick) {
                        Icon(Icons.Default.Close, contentDescription = "Minimize", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (mode == QuickNoteMode.LIST) {
                    QuickNoteList(
                        notes = allNotes,
                        onNoteClick = { 
                            activeNote = it
                            mode = QuickNoteMode.VIEW
                        },
                        onDeleteClick = { note ->
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { repository.deleteQuickNote(note) }
                        },
                        context = context
                    )
                } else if (mode == QuickNoteMode.VIEW) {
                    activeNote?.let {
                        val scrollState = rememberScrollState()
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(scrollState)) {
                            com.scholarvault.ui.components.SimpleMarkdownText(text = it.content)
                        }
                    }
                } else {
                    activeNote?.let {
                        QuickNoteEditor(
                            note = it,
                            repository = repository,
                            onContentChanged = { updatedNote -> activeNote = updatedNote },
                            showFormatting = isFullScreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickNoteList(
    notes: List<QuickNoteEntity>,
    onNoteClick: (QuickNoteEntity) -> Unit,
    onDeleteClick: (QuickNoteEntity) -> Unit,
    context: android.content.Context
) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quick notes. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.content.ifEmpty { "Empty Note" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = android.text.format.DateFormat.format("MMM dd", note.timestamp).toString(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (note.folder != null) {
                                    Text(" • ${note.folder}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                                if (note.tags.isNotEmpty()) {
                                    Text(" • ${note.tags.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        IconButton(onClick = { 
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, note.content)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteClick(note) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri, folderName: String): String? {
    try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/m4p" -> "m4a"
            "audio/m4a" -> "m4a"
            "audio/mp4" -> "mp4"
            else -> {
                val path = uri.path
                if (path != null) {
                    val dotIdx = path.lastIndexOf('.')
                    if (dotIdx != -1) path.substring(dotIdx + 1) else "bin"
                } else "bin"
            }
        }
        val fileName = "attached_${System.currentTimeMillis()}.$extension"
        val outputDir = java.io.File(context.filesDir, folderName)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = java.io.File(outputDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun QuickNoteEditor(note: QuickNoteEntity, repository: QuickNoteRepository, onContentChanged: (QuickNoteEntity) -> Unit, showFormatting: Boolean = false) {
    val context = LocalContext.current
    var contentValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(note.content)) }
    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var zoomLevel by remember { mutableStateOf(1f) }

    fun autoSave() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch(Dispatchers.IO) {
            delay(500) // debounce
            val updated = note.copy(content = contentValue.text, timestamp = Date())
            repository.insertQuickNote(updated)
            withContext(Dispatchers.Main) {
                onContentChanged(updated)
            }
        }
    }

    fun applyFormatting(prefix: String, suffix: String = prefix) {
        val sel = contentValue.selection
        val text = contentValue.text
        val newText = text.substring(0, sel.min) + prefix + text.substring(sel.min, sel.max) + suffix + text.substring(sel.max)
        val newCursor = sel.max + prefix.length
        contentValue = contentValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor, newCursor)
        )
        autoSave()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        TextField(
            value = contentValue,
            onValueChange = { 
                contentValue = it
                autoSave()
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.Transparent),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            placeholder = { Text("Jot down an idea in Markdown...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = (16 * zoomLevel).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = (24 * zoomLevel).sp)
        )

        if (showFormatting) {
            val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val data = result.data
                    val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                    if (!results.isNullOrEmpty()) {
                        val dictatedText = results[0] + " "
                        applyFormatting(dictatedText, "")
                    }
                }
            }

            val selectImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val savedPath = copyUriToInternalStorage(context, it, "attachments")
                    if (savedPath != null) {
                        applyFormatting("![Image]($savedPath)", "")
                    } else {
                        applyFormatting("![Image]($it)", "")
                    }
                }
            }

            val selectAudioLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val savedPath = copyUriToInternalStorage(context, it, "attachments")
                    if (savedPath != null) {
                        applyFormatting("![Audio]($savedPath)", "")
                    } else {
                        applyFormatting("![Audio]($it)", "")
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    IconButton(onClick = { zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0.5f) }) {
                        Text("A-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                item {
                    IconButton(onClick = { zoomLevel = (zoomLevel + 0.1f).coerceAtMost(3.0f) }) {
                        Text("A+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                item { Spacer(modifier = Modifier.width(4.dp)) }
                item {
                    IconButton(onClick = { selectImageLauncher.launch("image/*") }) {
                        Text("📷", fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { selectAudioLauncher.launch("audio/*") }) {
                        Text("🎵", fontSize = 18.sp)
                    }
                }
                item { Spacer(modifier = Modifier.width(4.dp)) }
                item {
                    IconButton(onClick = { applyFormatting("**") }) {
                        Text("B", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("*") }) {
                        Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("~~") }) {
                        Text("S", textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("### ", "") }) {
                        Text("H", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("- ", "") }) {
                        Icon(Icons.Default.List, contentDescription = "Bulleted List")
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("- [ ] ", "") }) {
                        Text("☑", fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("> ", "") }) {
                        Text("»", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("[Label](", ")") }) {
                        Text("🔗", fontSize = 16.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("`", "`") }) {
                        Text("{ }", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("```\n", "\n```") }) {
                        Text("</>", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                item {
                    IconButton(onClick = { applyFormatting("---\n", "") }) {
                        Text("—", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                item {
                    IconButton(onClick = { 
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        speechRecognizerLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Dictate Note")
                    }
                }
            }
        }
    }
}

enum class QuickNoteMode {
    LIST, VIEW, EDIT
}
